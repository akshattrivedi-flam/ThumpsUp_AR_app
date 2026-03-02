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
        val root = super.onCreateView(inflater, container, savedInstanceState)
        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        return root
    }

    override fun getSessionConfiguration(session: com.google.ar.core.Session): com.google.ar.core.Config {
        return com.google.ar.core.Config(session).apply {
            planeFindingMode = com.google.ar.core.Config.PlaneFindingMode.DISABLED
            focusMode = com.google.ar.core.Config.FocusMode.AUTO
            updateMode = com.google.ar.core.Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
    }
}
