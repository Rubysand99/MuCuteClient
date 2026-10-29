package com.mucheng.mucute.client.overlay

import android.os.Handler
import android.os.Looper
import com.mucheng.mucute.client.game.MacroManager

/**
 * Quản lý các nút macro trên màn hình. Lệnh `.macro button` chạy ở luồng xử lý packet nên mọi thao tác
 * với cửa sổ overlay đều được chuyển sang luồng UI.
 */
object MacroButtons {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Chỉ truy cập trên luồng UI (trừ lúc khởi tạo OverlayManager, cũng chạy trước khi có cửa sổ nào hiện).
    private val windows = HashMap<String, OverlayMacroButton>()

    private fun windowFor(name: String): OverlayMacroButton {
        return windows.getOrPut(name) { OverlayMacroButton(name) }
    }

    /** Các nút đã lưu, để OverlayManager hiện lại khi khởi động. */
    fun restoredWindows(): List<OverlayMacroButton> {
        return MacroManager.buttonNames().map { windowFor(it) }
    }

    /**
     * Hiện nút của macro [name] nếu chưa có, ngược lại ẩn đi.
     * Trả về true nếu nút sẽ được hiện, false nếu sẽ bị ẩn. Có thể gọi từ bất kỳ luồng nào.
     */
    fun toggle(name: String): Boolean {
        val show = !MacroManager.hasButton(name)
        if (show) {
            // Ghi nhận ngay (kèm vị trí mặc định) để các lệnh gọi liên tiếp thấy đúng trạng thái.
            MacroManager.addButton(name)
        }
        mainHandler.post {
            if (show) {
                OverlayManager.showOverlayWindow(windowFor(name))
            } else {
                hideNow(name)
            }
        }
        return show
    }

    /** Ẩn nút của macro [name] (dùng khi xoá macro). Có thể gọi từ bất kỳ luồng nào. */
    fun hide(name: String) {
        mainHandler.post { hideNow(name) }
    }

    private fun hideNow(name: String) {
        windows.remove(name)?.let { OverlayManager.dismissOverlayWindow(it) }
        MacroManager.removeButton(name)
    }

}
