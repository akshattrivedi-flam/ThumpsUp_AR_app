package com.thumsup.ar

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ModelRenderable
import com.thumsup.ar.databinding.ActivityArBinding

class ARActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArBinding
    private lateinit var arFragment: ARFragment

    private var session: Session? = null
    private var installRequested = false
    private var cylinderRenderable: ModelRenderable? = null
    private val anchoredNodes = mutableMapOf<Int, AnchorNode>()
    private var isVideoPrepared = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            resumeArSession()
        } else {
            Toast.makeText(this, getString(R.string.error_camera_permission), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = supportFragmentManager.findFragmentById(R.id.arFragment) as? ARFragment
        if (fragment == null) {
            Toast.makeText(this, getString(R.string.error_arcore_not_supported), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        arFragment = fragment

        CylinderNode.createRenderable(this)
            .thenAccept { renderable -> cylinderRenderable = renderable }

        arFragment.arSceneView.scene.addOnUpdateListener(::onUpdateFrame)
        setupOverlayVideo()
    }

    override fun onPause() {
        super.onPause()
        if (isVideoPrepared) {
            binding.videoOverlayView.pause()
        }
        arFragment.arSceneView.pause()
        session?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (isVideoPrepared && !binding.videoOverlayView.isPlaying) {
            binding.videoOverlayView.start()
        }
        if (!hasCameraPermission()) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        resumeArSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoOverlayView.stopPlayback()
        arFragment.arSceneView.scene.removeOnUpdateListener(::onUpdateFrame)
        anchoredNodes.values.forEach { node ->
            node.anchor?.detach()
            node.setParent(null)
        }
        anchoredNodes.clear()
        session?.close()
        session = null
    }

    private fun setupOverlayVideo() {
        val mediaController = MediaController(this).apply {
            setAnchorView(binding.videoOverlayView)
        }
        binding.videoOverlayView.setMediaController(mediaController)
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.stock_thumsup}")
        binding.videoOverlayView.setVideoURI(videoUri)
        binding.videoOverlayView.setOnPreparedListener { mediaPlayer ->
            isVideoPrepared = true
            mediaPlayer.isLooping = true
            binding.videoOverlayView.start()
        }
        binding.videoOverlayView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, getString(R.string.error_video_overlay), Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun resumeArSession() {
        val installStatus = runCatching {
            ArCoreApk.getInstance().requestInstall(this, !installRequested)
        }.getOrElse {
            Toast.makeText(this, getString(R.string.error_arcore_not_supported), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
            installRequested = true
            return
        }

        if (session == null) {
            session = try {
                Session(this)
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.error_arcore_not_supported), Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val createdSession = session ?: return
            configureSession(createdSession)
            arFragment.setSession(createdSession)
        }

        runCatching {
            session?.resume()
            arFragment.arSceneView.resume()
        }.onFailure {
            Toast.makeText(this, getString(R.string.error_arcore_not_supported), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun configureSession(arSession: Session) {
        val config = Config(arSession).apply {
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            augmentedImageDatabase = ImageDatabaseHelper.loadAugmentedImageDatabase(this@ARActivity, arSession)
            planeFindingMode = Config.PlaneFindingMode.DISABLED
        }
        arSession.configure(config)
    }

    private fun onUpdateFrame(@Suppress("UNUSED_PARAMETER") frameTime: FrameTime) {
        val frame = arFragment.arSceneView.arFrame ?: return
        val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

        updatedImages.forEach { image ->
            when (image.trackingState) {
                TrackingState.TRACKING -> {
                    if (!anchoredNodes.containsKey(image.index)) {
                        val anchor = image.createAnchor(image.centerPose)
                        addCanAnchor(image.index, anchor)
                    }
                }

                TrackingState.STOPPED -> removeCanAnchor(image.index)
                TrackingState.PAUSED -> Unit
            }
        }

        updateOverlayText()
    }

    private fun addCanAnchor(imageIndex: Int, anchor: Anchor) {
        val renderable = cylinderRenderable ?: return

        val anchorNode = AnchorNode(anchor).apply {
            setParent(arFragment.arSceneView.scene)
            addChild(CylinderNode(renderable))
        }

        anchoredNodes[imageIndex] = anchorNode
    }

    private fun removeCanAnchor(imageIndex: Int) {
        val node = anchoredNodes.remove(imageIndex) ?: return
        node.anchor?.detach()
        node.setParent(null)
    }

    private fun updateOverlayText() {
        val activeAnchor = anchoredNodes.values.firstOrNull()?.anchor
        if (activeAnchor == null || activeAnchor.trackingState != TrackingState.TRACKING) {
            binding.trackingOverlay.text = "Tracking: LOST"
            return
        }

        val pose = activeAnchor.pose
        val t = pose.translation
        val q = pose.rotationQuaternion
        val matrix = PoseUtils.toMatrix4x4(pose)
        val euler = PoseUtils.toEulerDegrees(pose)

        binding.trackingOverlay.text = buildString {
            append("Tracking: TRACKING\n")
            append("Position(m): x=${"%.3f".format(t[0])}, y=${"%.3f".format(t[1])}, z=${"%.3f".format(t[2])}\n")
            append("Quat: x=${"%.3f".format(q[0])}, y=${"%.3f".format(q[1])}, z=${"%.3f".format(q[2])}, w=${"%.3f".format(q[3])}\n")
            append("Euler(deg): r=${"%.1f".format(euler[0])}, p=${"%.1f".format(euler[1])}, y=${"%.1f".format(euler[2])}\n")
            append("M00=${"%.3f".format(matrix[0])}, M13=${"%.3f".format(matrix[13])}")
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
