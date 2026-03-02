package com.thumsup.ar

import com.google.ar.core.Pose
import kotlin.math.atan2
import kotlin.math.sqrt

object PoseUtils {
    fun toMatrix4x4(pose: Pose): FloatArray {
        return FloatArray(16).also { pose.toMatrix(it, 0) }
    }

    fun toEulerDegrees(pose: Pose): FloatArray {
        val q = pose.rotationQuaternion
        val x = q[0]
        val y = q[1]
        val z = q[2]
        val w = q[3]

        val sinrCosp = 2f * (w * x + y * z)
        val cosrCosp = 1f - 2f * (x * x + y * y)
        val roll = Math.toDegrees(atan2(sinrCosp, cosrCosp).toDouble()).toFloat()

        val sinp = 2f * (w * y - z * x)
        val pitch = Math.toDegrees(
            if (kotlin.math.abs(sinp) >= 1f) {
                (Math.copySign((Math.PI / 2), sinp.toDouble()))
            } else {
                kotlin.math.asin(sinp.toDouble())
            }
        ).toFloat()

        val sinyCosp = 2f * (w * z + x * y)
        val cosyCosp = 1f - 2f * (y * y + z * z)
        val yaw = Math.toDegrees(atan2(sinyCosp, cosyCosp).toDouble()).toFloat()

        return floatArrayOf(roll, pitch, yaw)
    }

    fun positionMagnitudeMeters(pose: Pose): Float {
        val t = pose.translation
        return sqrt(t[0] * t[0] + t[1] * t[1] + t[2] * t[2])
    }
}
