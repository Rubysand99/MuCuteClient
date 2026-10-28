package com.mucheng.mucute.client.game.module.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.world.Waypoint
import com.mucheng.mucute.client.game.world.WaypointManager
import com.mucheng.mucute.client.render.OverlayRenderer
import com.mucheng.mucute.client.render.RenderOverlayView
import com.mucheng.mucute.client.render.WorldProjector
import org.cloudburstmc.protocol.bedrock.packet.SetHealthPacket
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Hiển thị waypoint (tên + khoảng cách) trên màn hình, và tự đánh dấu chỗ chết.
 * Quản lý waypoint bằng lệnh `.waypoint` (xem CommandHandlerModule).
 */
class WaypointModule : Module("waypoints", ModuleCategory.Visual), OverlayRenderer {

    private val fov by floatValue("fov", 110f, 40f..110f)
    private val textSize by intValue("text_size", 24, 14..40)
    private val showDistance by boolValue("show_distance", true)
    private val deathWaypoint by boolValue("death_waypoint", true)

    // 0 = không giới hạn khoảng cách.
    private val maxDistance by intValue("max_distance", 0, 0..5000)

    companion object {
        const val DEATH_NAME = "death"
        const val PLAYER_EYE_HEIGHT = 1.62f

        private const val MARKER_SIZE = 10f
        private const val SCREEN_MARGIN = 40f
    }

    // Các đối tượng dùng lại giữa các frame để tránh cấp phát trong onDraw (chỉ chạy trên UI thread).
    private val projector = WorldProjector()
    private val hsv = floatArrayOf(0f, 0.7f, 1f)
    private val label = StringBuilder()
    private val marker = Path()

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private var dead = false

    override val isRendering: Boolean
        get() = isEnabled && isSessionCreated

    override fun onEnabled() {
        super.onEnabled()
        RenderOverlayView.requestRender()
    }

    override fun onDisabled() {
        super.onDisabled()
        RenderOverlayView.requestRender()
    }

    override fun onDisconnect(reason: String) {
        dead = false
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet !is SetHealthPacket) {
            return
        }

        if (packet.health > 0) {
            dead = false
        } else if (!dead && deathWaypoint) {
            // Một lần chết có thể kèm nhiều gói SetHealth, chỉ đánh dấu lần đầu.
            dead = true
            val player = session.localPlayer
            WaypointManager.put(
                Waypoint(
                    DEATH_NAME,
                    floor(player.posX).toInt(),
                    floor(player.posY - PLAYER_EYE_HEIGHT).toInt(),
                    floor(player.posZ).toInt(),
                    player.dimension
                )
            )
            session.displayClientMessage("§l§b[Waypoint] §r§7Saved death point §f$DEATH_NAME")
        }
    }

    override fun render(canvas: Canvas) {
        if (!isRendering) return

        val waypoints = WaypointManager.all
        if (waypoints.isEmpty()) return

        val player = session.localPlayer
        val screenWidth = canvas.width
        val screenHeight = canvas.height

        projector.update(
            player.vec3Position,
            player.rotationYaw,
            player.rotationPitch,
            fov,
            screenWidth,
            screenHeight
        )

        textPaint.textSize = textSize.toFloat()
        val limit = maxDistance.toFloat()

        for (waypoint in waypoints) {
            if (waypoint.dimension != player.dimension) continue

            // Vẽ ở giữa khối; khoảng cách tính từ chân người chơi.
            val wx = waypoint.x + 0.5f
            val wy = waypoint.y + 0.5f
            val wz = waypoint.z + 0.5f

            val dx = wx - player.posX
            val dy = wy - (player.posY - PLAYER_EYE_HEIGHT)
            val dz = wz - player.posZ
            val distance = sqrt(dx * dx + dy * dy + dz * dz)
            if (limit > 0f && distance > limit) continue

            if (!projector.project(wx, wy, wz)) continue

            val sx = projector.x
            val sy = projector.y
            if (sx < -SCREEN_MARGIN || sx > screenWidth + SCREEN_MARGIN ||
                sy < -SCREEN_MARGIN || sy > screenHeight + SCREEN_MARGIN
            ) {
                continue
            }

            hsv[0] = ((waypoint.name.hashCode() and 0x7fffffff) % 360).toFloat()
            val color = Color.HSVToColor(hsv)
            markerPaint.color = color
            textPaint.color = color

            // Hình thoi nhỏ đánh dấu vị trí.
            marker.rewind()
            marker.moveTo(sx, sy - MARKER_SIZE)
            marker.lineTo(sx + MARKER_SIZE, sy)
            marker.lineTo(sx, sy + MARKER_SIZE)
            marker.lineTo(sx - MARKER_SIZE, sy)
            marker.close()
            canvas.drawPath(marker, markerPaint)

            label.setLength(0)
            label.append(waypoint.name)
            if (showDistance) {
                label.append(" (").append(distance.toInt()).append("m)")
            }
            canvas.drawText(label, 0, label.length, sx, sy - MARKER_SIZE - 8f, textPaint)
        }
    }
}
