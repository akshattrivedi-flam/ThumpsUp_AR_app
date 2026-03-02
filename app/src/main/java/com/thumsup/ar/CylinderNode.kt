package com.thumsup.ar

import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import java.util.concurrent.CompletableFuture

class CylinderNode(private val cylinderRenderable: ModelRenderable) : Node() {

    init {
        val meshNode = Node().apply {
            renderable = cylinderRenderable
            localPosition = Vector3(0f, 0f, -RADIUS_METERS)
        }
        addChild(meshNode)
    }

    companion object {
        const val RADIUS_METERS = 0.033f
        const val HEIGHT_METERS = 0.1063f

        fun createRenderable(context: Context): CompletableFuture<ModelRenderable> {
            val color = Color(0.84f, 0.12f, 0.15f, 0.9f)
            return MaterialFactory.makeOpaqueWithColor(context, color)
                .thenApply { material ->
                    ShapeFactory.makeCylinder(
                        RADIUS_METERS,
                        HEIGHT_METERS,
                        Vector3.zero(),
                        material
                    ).apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                    }
                }
        }
    }
}
