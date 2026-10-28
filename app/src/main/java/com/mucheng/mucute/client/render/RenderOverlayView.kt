package com.mucheng.mucute.client.render

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.mucheng.mucute.client.game.ModuleManager

class RenderOverlayView(context: Context) : View(context) {

    companion object {
        @Volatile
        private var current: RenderOverlayView? = null

        /**
         * Yêu cầu vẽ lại overlay từ bất kỳ luồng nào.
         * Module gọi khi vừa bật/tắt để vòng vẽ (đã tự dừng khi không còn gì để vẽ) chạy lại.
         */
        fun requestRender() {
            current?.postInvalidate()
        }
    }

    // Danh sách module có vẽ là cố định nên chỉ tìm một lần, không lọc module mỗi frame.
    private val renderers: List<OverlayRenderer> by lazy(LazyThreadSafetyMode.NONE) {
        ModuleManager.modules.filterIsInstance<OverlayRenderer>()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        current = this
        invalidate() // Initial render
    }

    override fun onDetachedFromWindow() {
        if (current === this) {
            current = null
        }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var anyRendering = false
        for (renderer in renderers) {
            if (renderer.isRendering) {
                renderer.render(canvas)
                anyRendering = true
            }
        }

        if (anyRendering) {
            postInvalidateOnAnimation()
        }
    }
}
