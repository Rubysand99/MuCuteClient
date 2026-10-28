package com.mucheng.mucute.client.render

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.mucheng.mucute.client.game.ModuleManager
import com.mucheng.mucute.client.game.module.visual.ESPModule

class RenderOverlayView(context: Context) : View(context) {

    // Module ESP là cố định nên chỉ cần tìm một lần, không lọc danh sách module mỗi frame.
    private val espModule: ESPModule? by lazy(LazyThreadSafetyMode.NONE) {
        ModuleManager.modules.filterIsInstance<ESPModule>().firstOrNull()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate() // Initial render
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val esp = espModule ?: return
        if (esp.isEnabled && esp.isSessionCreated) { //isSessionCreated check
            esp.render(canvas)
            postInvalidateOnAnimation()
        }
    }
}
