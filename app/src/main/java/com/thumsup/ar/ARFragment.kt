package com.thumsup.ar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateSessionConfig(session: com.google.ar.core.Session): com.google.ar.core.Config {
        return com.google.ar.core.Config(session).apply {
            planeFindingMode = com.google.ar.core.Config.PlaneFindingMode.DISABLED
            focusMode = com.google.ar.core.Config.FocusMode.AUTO
            updateMode = com.google.ar.core.Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
    }
}
