package com.mucheng.mucute.client.game.module.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.entity.Entity
import com.mucheng.mucute.client.game.entity.Player
import com.mucheng.mucute.client.render.OverlayRenderer
import com.mucheng.mucute.client.render.RenderOverlayView
import com.mucheng.mucute.client.render.WorldProjector

class ESPModule : Module("esp", ModuleCategory.Visual), OverlayRenderer {
    companion object {
        private var renderView: RenderOverlayView? = null

        fun setRenderView(view: RenderOverlayView) {
            renderView = view
        }

        private const val BOX_WIDTH = 0.6f
        private const val BOX_HEIGHT = 1.8f // Standard player/entity height
        private const val PLAYER_EYE_HEIGHT = 1.62f
        private const val TEXT_PADDING = 8f

        // 12 cạnh của hộp 3D, mỗi cạnh là một cặp chỉ số đỉnh (đỉnh 0..7).
        private val BOX_EDGES = intArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0,  // Front face
            4, 5, 5, 6, 6, 7, 7, 4,  // Back face
            0, 4, 1, 5, 2, 6, 3, 7   // Connecting edges
        )
    }

    // Basic settings
    private val fov by floatValue("fov", 110f, 40f..110f)
    private val strokeWidth by floatValue("stroke_width", 2f, 1f..10f)
    private val colorRed by intValue("color_red", 255, 0..255)
    private val colorGreen by intValue("color_green", 0, 0..255)
    private val colorBlue by intValue("color_blue", 0, 0..255)

    // Display options
    private val showAllEntities by boolValue("show_all_entities", false)
    private val showDistance by boolValue("show_distance", true)
    private val showNames by boolValue("show_names", true)

    // Box style options
    private val use2DBox by boolValue("2d_box", false)
    private val use3DBox by boolValue("3d_box", true)
    private val useCornerBox by boolValue("corner_box", false)

    // Tracer options
    private val tracers by boolValue("tracers", false)
    private val tracerBottom by boolValue("tracer_bottom", true)
    private val tracerTop by boolValue("tracer_top", false)
    private val tracerCenter by boolValue("tracer_center", false)

    // Các đối tượng dùng lại giữa các frame để tránh cấp phát trong onDraw.
    // render() chỉ chạy trên UI thread nên không cần đồng bộ.
    private val boxPaint = Paint().apply { style = Paint.Style.STROKE }

    private val bgPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0) // Semi-transparent black background
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 4f // Thick outline
    }

    private val textPaint = Paint().apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    private val textBounds = Rect()
    private val bgRect = RectF()
    private val infoBuilder = StringBuilder()

    private val projector = WorldProjector()

    // Toạ độ màn hình của các đỉnh nhìn thấy được (đã dồn lại, tối đa 8 đỉnh).
    private val screenX = FloatArray(8)
    private val screenY = FloatArray(8)

    private val lineBuffer = FloatArray(BOX_EDGES.size * 2)
    private val cornerX = FloatArray(4)
    private val cornerY = FloatArray(4)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // ESP doesn't need to intercept packets
    }

    override fun onEnabled() {
        super.onEnabled()
        RenderOverlayView.requestRender()
    }

    override fun onDisabled() {
        super.onDisabled()
        RenderOverlayView.requestRender()
    }

    override val isRendering: Boolean
        get() = isEnabled && isSessionCreated

    private fun shouldRenderEntity(entity: Entity): Boolean {
        if (entity == session.localPlayer) return false
        if (!showAllEntities && entity !is Player) return false
        return true
    }

    override fun render(canvas: Canvas) {
        if (!isRendering) return

        val player = session.localPlayer
        val entities = session.level.entityMap.values

        if (entities.isEmpty()) return

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

        boxPaint.strokeWidth = strokeWidth
        boxPaint.color = Color.rgb(colorRed, colorGreen, colorBlue)
        textPaint.color = boxPaint.color

        for (entity in entities) {
            if (shouldRenderEntity(entity)) {
                drawEntityBox(entity, screenWidth, screenHeight, canvas)
            }
        }
    }

    private fun drawEntityBox(entity: Entity, screenWidth: Int, screenHeight: Int, canvas: Canvas) {
        val half = BOX_WIDTH / 2f
        val x = entity.posX
        val z = entity.posZ
        val yBottom = if (entity is Player) entity.posY - PLAYER_EYE_HEIGHT else entity.posY
        val yTop = yBottom + BOX_HEIGHT

        var minX = screenWidth.toFloat()
        var minY = screenHeight.toFloat()
        var maxX = 0f
        var maxY = 0f
        var visibleCount = 0

        // Đỉnh 0..7: mặt trước (z-) rồi mặt sau (z+), mỗi mặt đi theo thứ tự
        // dưới-trái, trên-trái, trên-phải, dưới-phải.
        for (i in 0 until 8) {
            val corner = i and 3
            val vx = if (corner >= 2) x + half else x - half
            val vy = if (corner == 1 || corner == 2) yTop else yBottom
            val vz = if (i >= 4) z + half else z - half

            if (!projector.project(vx, vy, vz)) continue

            val sx = projector.x
            val sy = projector.y
            screenX[visibleCount] = sx
            screenY[visibleCount] = sy
            visibleCount++

            if (sx < minX) minX = sx
            if (sy < minY) minY = sy
            if (sx > maxX) maxX = sx
            if (sy > maxY) maxY = sy
        }

        if (!(minX >= screenWidth || minY >= screenHeight || maxX <= 0 || maxY <= 0)) {
            when {
                use2DBox -> draw2DBox(canvas, minX, minY, maxX, maxY)
                use3DBox -> draw3DBox(canvas, visibleCount)
                useCornerBox -> drawCornerBox(canvas, minX, minY, maxX, maxY)
            }

            if (tracers) {
                drawTracer(canvas, screenWidth, screenHeight, minX, minY, maxX, maxY)
            }

            if (showNames || showDistance) {
                drawEntityInfo(canvas, entity, minX, minY, maxX)
            }
        }
    }

    private fun draw2DBox(canvas: Canvas, minX: Float, minY: Float, maxX: Float, maxY: Float) {
        val padding = boxPaint.strokeWidth / 2
        canvas.drawRect(
            minX + padding,
            minY + padding,
            maxX - padding,
            maxY - padding,
            boxPaint
        )
    }

    private fun draw3DBox(canvas: Canvas, visibleCount: Int) {
        // Thiếu đỉnh (có đỉnh nằm sau camera) thì không vẽ hộp 3D.
        if (visibleCount < 8) return

        val padding = boxPaint.strokeWidth / 2
        val maxDrawX = canvas.width - padding
        val maxDrawY = canvas.height - padding
        var floats = 0

        for (edge in 0 until BOX_EDGES.size / 2) {
            val start = BOX_EDGES[edge * 2]
            val end = BOX_EDGES[edge * 2 + 1]

            if (isOnScreen(screenX[start], screenY[start], canvas) &&
                isOnScreen(screenX[end], screenY[end], canvas)
            ) {
                lineBuffer[floats++] = screenX[start].coerceIn(padding, maxDrawX)
                lineBuffer[floats++] = screenY[start].coerceIn(padding, maxDrawY)
                lineBuffer[floats++] = screenX[end].coerceIn(padding, maxDrawX)
                lineBuffer[floats++] = screenY[end].coerceIn(padding, maxDrawY)
            }
        }

        if (floats > 0) {
            canvas.drawLines(lineBuffer, 0, floats, boxPaint)
        }
    }

    private fun isOnScreen(x: Float, y: Float, canvas: Canvas): Boolean {
        return x >= 0 && x <= canvas.width &&
                y >= 0 && y <= canvas.height
    }

    private fun drawCornerBox(canvas: Canvas, minX: Float, minY: Float, maxX: Float, maxY: Float) {
        val cornerLength = (maxX - minX) / 4

        cornerX[0] = minX; cornerY[0] = minY
        cornerX[1] = maxX; cornerY[1] = minY
        cornerX[2] = maxX; cornerY[2] = maxY
        cornerX[3] = minX; cornerY[3] = maxY

        for (i in 0 until 4) {
            val x = cornerX[i]
            val y = cornerY[i]
            val nextX = cornerX[(i + 1) % 4]
            val nextY = cornerY[(i + 1) % 4]
            val stubX = x + if (i % 2 == 0) cornerLength else -cornerLength

            // Horizontal lines
            canvas.drawLine(x, y, stubX, y, boxPaint)

            // Vertical lines
            canvas.drawLine(x, y, x, y + if (i < 2) cornerLength else -cornerLength, boxPaint)

            // Connect to next corner
            canvas.drawLine(stubX, y, nextX, nextY, boxPaint)
        }
    }

    private fun drawTracer(
        canvas: Canvas,
        screenWidth: Int,
        screenHeight: Int,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ) {
        val startX = screenWidth / 2f
        val startY = when {
            tracerBottom -> screenHeight.toFloat()
            tracerTop -> 0f
            tracerCenter -> screenHeight / 2f
            else -> screenHeight.toFloat()
        }

        canvas.drawLine(startX, startY, (minX + maxX) / 2, (minY + maxY) / 2, boxPaint)
    }

    private fun drawEntityInfo(canvas: Canvas, entity: Entity, minX: Float, minY: Float, maxX: Float) {
        infoBuilder.setLength(0)
        if (showNames && entity is Player) {
            infoBuilder.append(entity.username)
        }
        if (showDistance) {
            if (infoBuilder.isNotEmpty()) infoBuilder.append(" | ")
            // Làm tròn 1 chữ số thập phân, không phụ thuộc locale của máy.
            val tenths = (entity.distance(session.localPlayer) * 10f + 0.5f).toInt()
            infoBuilder.append(tenths / 10).append('.').append(tenths % 10).append('m')
        }
        if (infoBuilder.isEmpty()) return

        val info = infoBuilder.toString()
        val textX = (minX + maxX) / 2
        val textY = minY - 10

        textPaint.getTextBounds(info, 0, info.length, textBounds)

        bgRect.set(
            textX - textBounds.width() / 2 - TEXT_PADDING,
            textY - textBounds.height() - TEXT_PADDING,
            textX + textBounds.width() / 2 + TEXT_PADDING,
            textY + TEXT_PADDING
        )
        canvas.drawRoundRect(bgRect, 4f, 4f, bgPaint)

        canvas.drawText(info, textX, textY, outlinePaint)

        canvas.drawText(info, textX, textY, textPaint)
    }
}
