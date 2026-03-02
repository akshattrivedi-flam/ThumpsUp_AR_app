package com.thumsup.ar

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session

object ImageDatabaseHelper {
    private const val TAG = "ImageDatabaseHelper"

    fun loadAugmentedImageDatabase(context: Context, session: Session): AugmentedImageDatabase {
        val deserialized = runCatching {
            context.assets.open("augmented_image.imgdb").use { input ->
                AugmentedImageDatabase.deserialize(session, input)
            }
        }.getOrNull()

        if (deserialized != null) {
            return deserialized
        }

        Log.w(TAG, "Falling back to runtime image database creation from can_label.png")
        val bitmap = context.assets.open("can_label.png").use { input ->
            BitmapFactory.decodeStream(input)
                ?: error("Unable to decode can_label.png")
        }

        return AugmentedImageDatabase(session).apply {
            addImage("thumsup_can_label", bitmap, 0.20f)
        }
    }
}
