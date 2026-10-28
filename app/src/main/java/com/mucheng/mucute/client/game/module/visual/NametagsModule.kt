package com.mucheng.mucute.client.game.module.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.mucheng.mucute.client.game.FriendManager
import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.entity.Player
import com.mucheng.mucute.client.render.OverlayRenderer
import com.mucheng.mucute.client.render.RenderOverlayView
import com.mucheng.mucute.client.render.WorldProjector
import kotlin.math.sqrt

/**
 * Bảng tên trên đầu người chơi (giống Nametags của Meteor): tên lớn dễ đọc, khoảng cách và thanh máu.
 * Thanh máu chỉ hiện khi server gửi attribute "minecraft:health" của người chơi đó.
 */
class NametagsModule : Module("nametags", ModuleCategory.Visual), OverlayRenderer {

    private val textSize by intValue("text_size", 26, 14..44)
    private val maxDistance by intValue("max_distance", 64, 8..128)
    private val showHealth by boolValue("show_health", true)
    private val showDistance by boolValue("show_distance", true)
    private val scaleWithDistance by boolValue("scale_with_distance", true)
    private val fov by floatValue("fov", 110f, 40f..110f)

    private companion object {
        const val HEALTH_ATTRIBUTE = "minecraft:health"

        // posY của người chơi là độ cao mắt (cách chân 1.62); bảng tên nằm trên đỉnh đầu một chút.
        const val TAG_HEIGHT_ABOVE_EYES = 0.65f

        const val PADDING = 8f
        const val BAR_HEIGHT = 8f
        const val BAR_GAP = 6f
        const val MIN_SCALE = 0.55f
        const val MAX_SCALE = 1.4f
        const val SCALE_REFERENCE_DISTANCE = 12f
        const val SCREEN_MARGIN = 120f

        val FRIEND_COLOR = Color.rgb(60, 220, 90)
    }

    // Các đối tượng dùng lại giữa các frame để tránh cấp phát trong onDraw (chỉ chạy trên UI thread).
    private val projector = WorldProjector()
    private val textBounds = Rect()
    private val background = RectF()
    private val label = StringBuilder()
    private val hsv = floatArrayOf(0f, 1f, 1f)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(150, 0, 0, 0)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override val isRendering: Boolean
        get() = isEnabled && isSessionCreated

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // Chỉ vẽ, không đụng tới packet.
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

        val local = session.localPlayer
        val entities = session.level.entityMap.values
        if (entities.isEmpty()) return

        val width = canvas.width
        val height = canvas.height

        projector.update(
            local.vec3Position,
            local.rotationYaw,
            local.rotationPitch,
            fov,
            width,
            height
        )

        val limit = maxDistance.toFloat()

        for (entity in entities) {
            if (entity !is Player || entity === local) continue

            val dx = entity.posX - local.posX
            val dy = entity.posY - local.posY
            val dz = entity.posZ - local.posZ
            val distance = sqrt(dx * dx + dy * dy + dz * dz)
            if (distance > limit) continue

            if (!projector.project(entity.posX, entity.posY + TAG_HEIGHT_ABOVE_EYES, entity.posZ)) continue

            val sx = projector.x
            val sy = projector.y
            if (sx < -SCREEN_MARGIN || sx > width + SCREEN_MARGIN ||
                sy < -SCREEN_MARGIN || sy > height + SCREEN_MARGIN
            ) {
                continue
            }

            val scale = if (scaleWithDistance) {
                (SCALE_REFERENCE_DISTANCE / distance.coerceAtLeast(1f)).coerceIn(MIN_SCALE, MAX_SCALE)
            } else {
                1f
            }

            drawTag(canvas, entity, distance, sx, sy, scale)
        }
    }

    private fun drawTag(canvas: Canvas, player: Player, distance: Float, x: Float, y: Float, scale: Float) {
        label.setLength(0)
        label.append(player.displayName)
        if (showDistance) {
            label.append(" [").append(distance.toInt()).append("m]")
        }

        val health = if (showHealth) player.attributes[HEALTH_ATTRIBUTE] else null
        val maxHealth = health?.maximum ?: 0f
        val hasBar = health != null && maxHealth > 0f

        textPaint.textSize = textSize * scale
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        val pad = PADDING * scale
        val barHeight = BAR_HEIGHT * scale
        val barGap = BAR_GAP * scale
        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()

        val boxWidth = textWidth + pad * 2
        val boxHeight = textHeight + pad * 2 + if (hasBar) barGap + barHeight else 0f

        // Điểm neo (x, y) là đáy của bảng, bảng nằm phía trên đầu.
        val left = x - boxWidth / 2
        val top = y - boxHeight
        background.set(left, top, left + boxWidth, y)
        canvas.drawRoundRect(background, 6f * scale, 6f * scale, backgroundPaint)

        textPaint.color = if (FriendManager.isFriend(player.username)) FRIEND_COLOR else Color.WHITE
        canvas.drawText(label, 0, label.length, x, top + pad + textHeight, textPaint)

        if (hasBar) {
            val fraction = (health!!.value / maxHealth).coerceIn(0f, 1f)
            val barLeft = left + pad
            val barRight = left + boxWidth - pad
            val barTop = y - pad - barHeight

            // Nền thanh máu, rồi phần máu còn lại (đỏ -> xanh theo tỉ lệ).
            barPaint.color = Color.argb(120, 255, 255, 255)
            canvas.drawRect(barLeft, barTop, barRight, barTop + barHeight, barPaint)

            hsv[0] = 120f * fraction
            barPaint.color = Color.HSVToColor(hsv)
            canvas.drawRect(barLeft, barTop, barLeft + (barRight - barLeft) * fraction, barTop + barHeight, barPaint)
        }
    }
}
