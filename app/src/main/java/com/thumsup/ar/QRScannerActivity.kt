package com.thumsup.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.thumsup.ar.databinding.ActivityQrScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    private var camera: Camera? = null
    private val isProcessingFrame = AtomicBoolean(false)
    private val hasHandledScan = AtomicBoolean(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, getString(R.string.error_camera_permission), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        routeDeepLinkIfPresent(intent)

        binding.playDefaultButton.setOnClickListener {
            launchAr("thumsup300")
        }

        binding.flashButton.setOnClickListener {
            val cam = camera ?: return@setOnClickListener
            if (cam.cameraInfo.hasFlashUnit()) {
                val enabled = cam.cameraInfo.torchState.value == 1
                cam.cameraControl.enableTorch(!enabled)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasHandledScan.set(false)
        if (hasCameraPermission()) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching { ProcessCameraProvider.getInstance(this).get().unbindAll() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeDeepLinkIfPresent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner.close()
        cameraExecutor.shutdown()
    }

    private fun routeDeepLinkIfPresent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val product = DeepLinkHandler.parseProductFromIntent(intent)
        if (product == null) {
            Toast.makeText(this, getString(R.string.error_invalid_link), Toast.LENGTH_LONG).show()
            return
        }
        launchAr(product)
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor, ::analyzeFrame)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (hasHandledScan.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null || !isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstOrNull()?.rawValue ?: return@addOnSuccessListener
                if (hasHandledScan.compareAndSet(false, true)) {
                    runOnUiThread {
                        onQrDetected(rawValue)
                    }
                }
            }
            .addOnFailureListener {
                // Continue scanning frames.
            }
            .addOnCompleteListener {
                isProcessingFrame.set(false)
                imageProxy.close()
            }
    }

    private fun onQrDetected(rawValue: String) {
        val product = DeepLinkHandler.parseProductFromQr(rawValue)
        if (product == null) {
            hasHandledScan.set(false)
            Toast.makeText(this, getString(R.string.error_invalid_link), Toast.LENGTH_SHORT).show()
            return
        }
        launchAr(product)
    }

    private fun launchAr(product: String?) {
        startActivity(DeepLinkHandler.buildArIntent(this, product))
        finish()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
