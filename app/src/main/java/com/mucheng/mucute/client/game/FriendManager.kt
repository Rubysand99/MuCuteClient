package com.mucheng.mucute.client.game

import android.content.Context
import com.mucheng.mucute.client.application.AppContext
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Danh sách bạn bè (theo tên người chơi, không phân biệt hoa thường).
 * Các hành động tấn công tự động bỏ qua bạn bè; ESP tô màu riêng cho họ.
 * Lưu trong SharedPreferences "settings", dùng chung cho mọi server.
 */
object FriendManager {

    private const val KEY = "friends"

    // Lưu dạng chữ thường để so khớp; giữ thêm tên gốc để hiển thị.
    private val displayNames = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val keys = CopyOnWriteArraySet<String>()

    init {
        val saved = AppContext.instance
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()
        for (name in saved) {
            keys.add(name.lowercase())
            displayNames[name.lowercase()] = name
        }
    }

    val all: List<String>
        get() = displayNames.values.sortedBy { it.lowercase() }

    fun isFriend(name: String): Boolean = name.isNotEmpty() && keys.contains(name.lowercase())

    /** Trả về false nếu đã là bạn bè. */
    @Synchronized
    fun add(name: String): Boolean {
        val key = name.lowercase()
        if (!keys.add(key)) {
            return false
        }
        displayNames[key] = name
        save()
        return true
    }

    @Synchronized
    fun remove(name: String): Boolean {
        val key = name.lowercase()
        if (!keys.remove(key)) {
            return false
        }
        displayNames.remove(key)
        save()
        return true
    }

    @Synchronized
    fun clear() {
        keys.clear()
        displayNames.clear()
        save()
    }

    private fun save() {
        AppContext.instance
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, displayNames.values.toSet())
            .apply()
    }

}
