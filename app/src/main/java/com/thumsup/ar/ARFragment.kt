package com.thumsup.ar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment

class ARFragment : ArFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = requireNotNull(super.onCreateView(inflater, container, savedInstanceState))
        instructionsController.setVisible(false)
        instructionsController.setEnabled(com.google.ar.sceneform.ux.InstructionsController.TYPE_PLANE_DISCOVERY, false)
        return root
    }

    override fun onCreateSessionConfig(session: Session): Config {
        return Config(session).apply {
            planeFindingMode = Config.PlaneFindingMode.DISABLED
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            augmentedImageDatabase = runCatching {
                ImageDatabaseHelper.loadAugmentedImageDatabase(requireContext(), session)
            }.getOrElse {
                Toast.makeText(requireContext(), R.string.error_failed_start_ar_session, Toast.LENGTH_LONG).show()
                null
            }
        }
    }
}
