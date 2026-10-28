package com.mucheng.mucute.client.render

import android.graphics.Canvas

/**
 * Module có vẽ lên lớp overlay (ESP, HUD, Waypoint...).
 * RenderOverlayView gọi [render] mỗi frame khi [isRendering] là true.
 */
interface OverlayRenderer {

    /** True khi module đang bật và có phiên game để vẽ. */
    val isRendering: Boolean

    fun render(canvas: Canvas)

}
