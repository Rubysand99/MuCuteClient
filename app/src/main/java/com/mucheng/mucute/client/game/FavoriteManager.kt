package com.mucheng.mucute.client.game

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.mucheng.mucute.client.application.AppContext

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

}
