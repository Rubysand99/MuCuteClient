package com.mucheng.mucute.client.game.world

import android.util.Log
import com.mucheng.mucute.client.application.AppContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Điểm đánh dấu theo toạ độ khối. [dimension]: 0 = Overworld, 1 = Nether, 2 = End.
 */
@Serializable
data class Waypoint(
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val dimension: Int
)

@Serializable
private data class WaypointFile(val waypoints: List<Waypoint> = emptyList())

/**
 * Quản lý waypoint theo từng server (mỗi server một file trong filesDir/waypoints).
 * An toàn khi gọi từ nhiều luồng (luồng packet, luồng vẽ overlay).
 */
object WaypointManager {

    const val MAX_NAME_LENGTH = 32

    private const val TAG = "WaypointManager"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val waypoints = CopyOnWriteArrayList<Waypoint>()

    @Volatile
    private var serverKey = "default"

    val all: List<Waypoint>
        get() = waypoints

    /** Chọn server hiện tại và nạp waypoint đã lưu của server đó. */
    @Synchronized
    fun setServer(host: String, port: Int) {
        serverKey = "$host-$port".replace(Regex("[^A-Za-z0-9._-]"), "_")
        load()
    }

    fun find(name: String): Waypoint? {
        return waypoints.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    /** Trả về false nếu tên đã tồn tại. */
    @Synchronized
    fun add(waypoint: Waypoint): Boolean {
        if (find(waypoint.name) != null) {
            return false
        }
        waypoints.add(waypoint)
        save()
        return true
    }

    /** Thêm hoặc thay thế waypoint cùng tên (dùng cho waypoint tự động như "death"). */
    @Synchronized
    fun put(waypoint: Waypoint) {
        waypoints.removeAll { it.name.equals(waypoint.name, ignoreCase = true) }
        waypoints.add(waypoint)
        save()
    }

    @Synchronized
    fun remove(name: String): Boolean {
        val removed = waypoints.removeAll { it.name.equals(name, ignoreCase = true) }
        if (removed) {
            save()
        }
        return removed
    }

    @Synchronized
    fun clear() {
        waypoints.clear()
        save()
    }

    private fun file(): File {
        val dir = AppContext.instance.filesDir.resolve("waypoints")
        dir.mkdirs()
        return dir.resolve("$serverKey.json")
    }

    private fun load() {
        waypoints.clear()
        try {
            val file = file()
            if (file.exists()) {
                waypoints.addAll(json.decodeFromString<WaypointFile>(file.readText()).waypoints)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load waypoints for $serverKey", e)
        }
    }

    private fun save() {
        try {
            val target = file()
            val temp = File(target.parentFile, target.name + ".tmp")
            temp.writeText(json.encodeToString(WaypointFile(waypoints.toList())))
            if (!temp.renameTo(target)) {
                target.writeText(temp.readText())
                temp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save waypoints for $serverKey", e)
        }
    }

}
