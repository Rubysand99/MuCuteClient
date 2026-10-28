package com.mucheng.mucute.client.render

import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * Chiếu điểm trong thế giới Minecraft lên màn hình overlay, dựa trên vị trí/góc nhìn của người chơi.
 * Dùng chung cho ESP, Waypoint... Đối tượng được dùng lại giữa các frame (không cấp phát trong onDraw)
 * và chỉ được dùng trên UI thread.
 */
class WorldProjector {

    /** Toạ độ màn hình của điểm vừa chiếu thành công bằng [project]. */
    var x = 0f
        private set

    var y = 0f
        private set

    private var width = 0
    private var height = 0

    // Hàng 0, 1 và 3 của ma trận view-projection (mỗi hàng 4 số).
    private val m = FloatArray(12)

    fun update(
        position: Vector3f,
        yaw: Float,
        pitch: Float,
        fov: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        width = screenWidth
        height = screenHeight

        val viewProj = Matrix4f.createPerspective(
            fov,
            screenWidth.toFloat() / screenHeight,
            0.1f,
            128f
        ).mul(
            Matrix4f.createTranslation(position)
                .mul(rotateY(-yaw - 180))
                .mul(rotateX(-pitch))
                .invert()
        )

        for (column in 0 until 4) {
            m[column] = viewProj.get(0, column)
            m[4 + column] = viewProj.get(1, column)
            m[8 + column] = viewProj.get(3, column)
        }
    }

    /**
     * Chiếu (px, py, pz) lên màn hình, kết quả nằm ở [x]/[y].
     * Trả về false nếu điểm nằm sau camera.
     */
    fun project(px: Float, py: Float, pz: Float): Boolean {
        val w = m[8] * px + m[9] * py + m[10] * pz + m[11]
        if (w < 0.01f) return false

        val inverseW = 1f / w

        x = width / 2f + (0.5f * ((m[0] * px + m[1] * py + m[2] * pz + m[3]) * inverseW) * width + 0.5f)
        y = height / 2f - (0.5f * ((m[4] * px + m[5] * py + m[6] * pz + m[7]) * inverseW) * height + 0.5f)
        return true
    }

    private fun rotateX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(
            1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun rotateY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(
            c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

}
