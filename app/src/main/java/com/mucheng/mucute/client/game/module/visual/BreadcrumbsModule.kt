package com.mucheng.mucute.client.game.module.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.render.OverlayRenderer
import com.mucheng.mucute.client.render.RenderOverlayView
import com.mucheng.mucute.client.render.WorldProjector
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

/**
 * Vẽ vệt đường đi gần đây của người chơi (giống Breadcrumbs của Meteor).
 * Luồng packet ghi điểm, luồng vẽ overlay đọc; hai bên trao đổi qua một vòng đệm có khoá.
 */
class BreadcrumbsModule : Module("breadcrumbs", ModuleCategory.Visual), OverlayRenderer {

    private val maxPoints by intValue("max_points", 300, 20..MAX_CAPACITY)
    private val minDistance by floatValue("min_distance", 0.5f, 0.2f..4f)
    private val strokeWidth by floatValue("stroke_width", 3f, 1f..10f)
    private val fov by floatValue("fov", 110f, 40f..110f)
    private val colorRed by intValue("color_red", 80, 0..255)
    private val colorGreen by intValue("color_green", 200, 0..255)
    private val colorBlue by intValue("color_blue", 255, 0..255)

    private companion object {
        const val MAX_CAPACITY = 2000
        const val PLAYER_EYE_HEIGHT = 1.62f
        const val FADE_START_ALPHA = 40

        // Dịch chuyển ngang xa hơn 32 khối giữa hai điểm liên tiếp coi là teleport.
        const val TELEPORT_DISTANCE_SQ = 32f * 32f
    }

    // Vòng đệm toạ độ (x, y, z liên tiếp). Mọi truy cập đều qua `lock`.
    private val lock = Any()
    private val ring = FloatArray(MAX_CAPACITY * 3)
    private var head = 0
    private var size = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    // Bản sao theo thứ tự cũ -> mới dùng khi vẽ (chỉ UI thread đụng tới).
    private val snapshot = FloatArray(MAX_CAPACITY * 3)
    private val projector = WorldProjector()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override val isRendering: Boolean
        get() = isEnabled && isSessionCreated

    override fun onEnabled() {
        super.onEnabled()
        clearPoints()
        RenderOverlayView.requestRender()
    }

    override fun onDisabled() {
        super.onDisabled()
        clearPoints()
        RenderOverlayView.requestRender()
    }

    override fun onDisconnect(reason: String) {
        clearPoints()
    }

    private fun clearPoints() {
        synchronized(lock) {
            head = 0
            size = 0
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet as? PlayerAuthInputPacket ?: return

        val x = packet.position.x
        // Vị trí trong packet là mắt người chơi; ghi ở chân để vệt nằm dưới đất.
        val y = packet.position.y - PLAYER_EYE_HEIGHT
        val z = packet.position.z

        synchronized(lock) {
            if (size > 0) {
                val dx = x - lastX
                val dy = y - lastY
                val dz = z - lastZ
                // Đứng yên hoặc di chuyển rất ít thì không ghi thêm điểm.
                if (dx * dx + dy * dy + dz * dz < minDistance * minDistance) {
                    return
                }

                // Dịch chuyển xa bất thường (đổi dimension, teleport) thì bỏ vệt cũ để không kẻ đường nối xuyên bản đồ.
                if (dx * dx + dz * dz > TELEPORT_DISTANCE_SQ) {
                    head = 0
                    size = 0
                }
            }

            ring[head * 3] = x
            ring[head * 3 + 1] = y
            ring[head * 3 + 2] = z
            head = (head + 1) % MAX_CAPACITY
            if (size < MAX_CAPACITY) size++
            // maxPoints có thể nhỏ hơn dung lượng vòng đệm; khi vẽ chỉ lấy đoạn mới nhất.
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun render(canvas: Canvas) {
        if (!isRendering) return

        // Sao chép đoạn mới nhất (tối đa maxPoints điểm) ra theo thứ tự cũ -> mới.
        var count: Int
        synchronized(lock) {
            count = minOf(size, maxPoints)
            val start = (head - count + MAX_CAPACITY) % MAX_CAPACITY
            for (i in 0 until count) {
                val src = ((start + i) % MAX_CAPACITY) * 3
                snapshot[i * 3] = ring[src]
                snapshot[i * 3 + 1] = ring[src + 1]
                snapshot[i * 3 + 2] = ring[src + 2]
            }
        }
        if (count < 2) return

        val player = session.localPlayer
        projector.update(
            player.vec3Position,
            player.rotationYaw,
            player.rotationPitch,
            fov,
            canvas.width,
            canvas.height
        )

        linePaint.strokeWidth = strokeWidth
        val baseR = colorRed
        val baseG = colorGreen
        val baseB = colorBlue

        var prevVisible = false
        var prevX = 0f
        var prevY = 0f

        for (i in 0 until count) {
            val visible = projector.project(snapshot[i * 3], snapshot[i * 3 + 1], snapshot[i * 3 + 2])
            val sx = projector.x
            val sy = projector.y

            if (visible && prevVisible) {
                // Đoạn càng cũ càng mờ.
                val alpha = FADE_START_ALPHA + (255 - FADE_START_ALPHA) * i / (count - 1)
                linePaint.color = Color.argb(alpha, baseR, baseG, baseB)
                canvas.drawLine(prevX, prevY, sx, sy, linePaint)
            }

            prevVisible = visible
            prevX = sx
            prevY = sy
        }
    }
}
