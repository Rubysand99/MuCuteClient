package com.mucheng.mucute.client.game

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.mucheng.mucute.client.application.AppContext
import com.mucheng.mucute.client.overlay.OverlayManager

/**
 * Danh sách module yêu thích (hiện đầu danh sách trong menu). Lưu theo tên module trong
 * SharedPreferences "settings", độc lập với profile cấu hình.
 * Dùng state của Compose nên giao diện tự cập nhật khi đổi.
 */
object FavoriteManager {

    private const val KEY = "favorite_modules"

    private fun preferences() =
        AppContext.instance.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val names = mutableStateListOf<String>().also {
        it.addAll(preferences().getStringSet(KEY, emptySet()).orEmpty())
    }

    fun isFavorite(moduleName: String) = names.contains(moduleName)

    fun toggle(moduleName: String) {
        if (!names.remove(moduleName)) {
            names.add(moduleName)
        }
        preferences().edit().putStringSet(KEY, names.toSet()).apply()
    }


    /** Các module yêu thích (không tính module ẩn), theo thứ tự đăng ký. Đọc state nên dùng được trong composition. */
    fun favoriteModules(): List<Module> {
        return ModuleManager.modules.filter { !it.private && names.contains(it.name) }
    }

    /** True khi có ít nhất một module yêu thích và tất cả đều đang có nút tắt/bật trên màn hình. */
    fun areAllShortcutsShown(): Boolean {
        val favorites = favoriteModules()
        return favorites.isNotEmpty() && favorites.all { it.isShortcutDisplayed }
    }

    /**
     * Nếu còn module yêu thích chưa có nút thì hiện nút cho tất cả (xếp lưới dọc theo cạnh trái, qua cột mới khi hết
     * chiều cao màn hình); ngược lại ẩn hết nút của các module yêu thích. Gọi trên UI thread.
     * Nút đã hiện sẵn giữ nguyên chỗ người dùng đã kéo tới.
     */
    fun toggleFavoriteShortcuts() {
        val favorites = favoriteModules()
        if (favorites.isEmpty()) return

        if (favorites.all { it.isShortcutDisplayed }) {
            for (module in favorites) {
                module.isShortcutDisplayed = false
                OverlayManager.dismissOverlayWindow(module.overlayShortcutButton)
            }
            return
        }

        val metrics = AppContext.instance.resources.displayMetrics
        val density = metrics.density
        val margin = (16 * density).toInt()
        val top = (72 * density).toInt()
        val rowStep = (64 * density).toInt()
        val columnStep = (150 * density).toInt()
        val rowsPerColumn = ((metrics.heightPixels - top - margin) / rowStep).coerceAtLeast(1)

        favorites.forEachIndexed { index, module ->
            if (module.isShortcutDisplayed) return@forEachIndexed

            module.overlayShortcutButton.moveTo(
                margin + (index / rowsPerColumn) * columnStep,
                top + (index % rowsPerColumn) * rowStep
            )
            module.isShortcutDisplayed = true
            OverlayManager.showOverlayWindow(module.overlayShortcutButton)
        }
    }

}
