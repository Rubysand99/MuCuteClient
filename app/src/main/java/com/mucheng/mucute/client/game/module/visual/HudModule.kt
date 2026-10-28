package com.mucheng.mucute.client.game.module.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.ModuleManager
import com.mucheng.mucute.client.game.entity.Player
import com.mucheng.mucute.client.render.OverlayRenderer
import com.mucheng.mucute.client.render.RenderOverlayView
import com.mucheng.mucute.client.util.translatedSelf
import kotlin.math.floor

/**
 * HUD vẽ trên lớp overlay: danh sách module đang bật (kiểu ArrayList của Meteor/Wurst),
 * toạ độ, hướng nhìn và số người chơi gần đó.
 */
class HudModule : Module("hud", ModuleCategory.Visual), OverlayRenderer {

    private val showModuleList by boolValue("show_module_list", true)
    private val showCoordinates by boolValue("show_coordinates", true)
    private val showDirection by boolValue("show_direction", true)
    private val showNearbyPlayers by boolValue("show_nearby_players", true)
    private val rightAlign by boolValue("right_align", true)
    private val rainbow by boolValue("rainbow", false)
    private val textSize by intValue("text_size", 26, 14..48)
    private val colorRed by intValue("color_red", 90, 0..255)
    private val colorGreen by intValue("color_green", 200, 0..255)
    private val colorBlue by intValue("color_blue", 255, 0..255)

    private companion object {
        const val MARGIN = 16f
        const val NEARBY_PLAYER_RANGE = 64f
        const val RAINBOW_STEP = 14f

        // Bedrock: yaw 0 = nam (+Z), 90 = tây (-X), 180 = bắc (-Z), 270 = đông (+X).
        val DIRECTIONS = arrayOf("S (+Z)", "W (-X)", "N (-Z)", "E (+X)")
    }

    // Các đối tượng dùng lại giữa các frame để tránh cấp phát trong onDraw (chỉ chạy trên UI thread).
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val entries = ArrayList<String>()
    private val byLengthDescending = Comparator<String> { a, b -> b.length - a.length }
    private val hsv = FloatArray(3)
    private val infoBuilder = StringBuilder()

    override val isRendering: Boolean
        get() = isEnabled && isSessionCreated

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // HUD chỉ vẽ, không đụng tới packet.
    }

    override fun onEnabled() {
        super.onEnabled()
        RenderOverlayView.requestRender()
    }

    override fun onDisabled() {
        super.onDisabled()
        RenderOverlayView.requestRender()
    }

    override fun render(canvas: Canvas) {
        if (!isRendering) return

        textPaint.textSize = textSize.toFloat()
        val lineHeight = textPaint.fontSpacing
        val baseColor = Color.rgb(colorRed, colorGreen, colorBlue)

        // Khối thông tin ở góc trên bên trái.
        var infoY = MARGIN - textPaint.ascent()
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = baseColor

        val player = session.localPlayer

        if (showCoordinates) {
            infoBuilder.setLength(0)
            infoBuilder.append("XYZ: ")
                .append(floor(player.posX).toInt()).append(' ')
                .append(floor(player.posY).toInt()).append(' ')
                .append(floor(player.posZ).toInt())
            canvas.drawText(infoBuilder, 0, infoBuilder.length, MARGIN, infoY, textPaint)
            infoY += lineHeight
        }

        if (showDirection) {
            val yaw = ((player.rotationYaw % 360f) + 360f) % 360f
            val index = ((yaw + 45f) / 90f).toInt() % 4
            infoBuilder.setLength(0)
            infoBuilder.append("Facing: ").append(DIRECTIONS[index])
            canvas.drawText(infoBuilder, 0, infoBuilder.length, MARGIN, infoY, textPaint)
            infoY += lineHeight
        }

        if (showNearbyPlayers) {
            var nearby = 0
            val rangeSq = NEARBY_PLAYER_RANGE * NEARBY_PLAYER_RANGE
            for (entity in session.level.entityMap.values) {
                if (entity is Player && entity.distanceSq(player) <= rangeSq) {
                    nearby++
                }
            }
            infoBuilder.setLength(0)
            infoBuilder.append("Players (").append(NEARBY_PLAYER_RANGE.toInt()).append("m): ").append(nearby)
            canvas.drawText(infoBuilder, 0, infoBuilder.length, MARGIN, infoY, textPaint)
            infoY += lineHeight
        }

        if (!showModuleList) return

        // Danh sách module đang bật, dài nhất lên trên.
        entries.clear()
        val modules = ModuleManager.modules
        for (i in modules.indices) {
            val module = modules[i]
            if (module.isEnabled && !module.private && module !== this) {
                entries.add(module.name.translatedSelf)
            }
        }
        if (entries.isEmpty()) return
        entries.sortWith(byLengthDescending)

        val listX: Float
        var listY: Float
        if (rightAlign) {
            textPaint.textAlign = Paint.Align.RIGHT
            listX = canvas.width - MARGIN
            listY = MARGIN - textPaint.ascent()
        } else {
            textPaint.textAlign = Paint.Align.LEFT
            listX = MARGIN
            listY = infoY + lineHeight / 2
        }

        val time = (System.currentTimeMillis() % 6000L) / 6000f * 360f
        for (i in entries.indices) {
            textPaint.color = if (rainbow) {
                hsv[0] = (time + i * RAINBOW_STEP) % 360f
                hsv[1] = 0.65f
                hsv[2] = 1f
                Color.HSVToColor(hsv)
            } else {
                baseColor
            }
            canvas.drawText(entries[i], listX, listY, textPaint)
            listY += lineHeight
        }
    }
}
