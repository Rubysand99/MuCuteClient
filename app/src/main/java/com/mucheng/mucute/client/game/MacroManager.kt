package com.mucheng.mucute.client.game

import android.util.Log
import com.mucheng.mucute.client.application.AppContext
import com.mucheng.mucute.client.game.module.misc.CommandHandlerModule
import kotlinx.serialization.json.Json
import org.cloudburstmc.protocol.bedrock.data.command.CommandOriginData
import org.cloudburstmc.protocol.bedrock.data.command.CommandOriginType
import org.cloudburstmc.protocol.bedrock.packet.CommandRequestPacket
import java.io.File
import java.util.TreeMap
import java.util.UUID

/**
 * Macro: một chuỗi bước có đặt tên, chạy bằng `.macro run <tên>`.
 *
 * Các loại bước:
 *  - `on <module>` / `off <module>`  bật hoặc tắt hẳn một module
 *  - `.lệnh ...`                      lệnh client (ví dụ `.waypoint add base`)
 *  - `/lệnh ...`                      lệnh server (gửi như người chơi gõ)
 *  - `wait <ms>`                      chờ (tối đa [MAX_WAIT_MS])
 *
 * Macro được lưu trong filesDir/macros.json. Một lúc chỉ chạy một macro, và macro không được gọi `.macro`
 * (tránh vòng lặp vô hạn). Macro dừng khi ngắt kết nối hoặc khi `.macro stop`.
 */
object MacroManager {

    const val MAX_STEPS = 50
    const val MAX_WAIT_MS = 60_000L

    private const val TAG = "MacroManager"

    private val nameRegex = Regex("^[A-Za-z0-9_-]{1,32}$")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Sắp theo tên để `.macro list` ổn định. Mọi truy cập đều qua khoá của object.
    private val macros = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)

    private var loaded = false

    @Volatile
    private var runner: Thread? = null

    val isRunning: Boolean
        get() = runner?.isAlive == true

    fun isValidName(name: String) = nameRegex.matches(name)

    private fun file(): File = AppContext.instance.filesDir.resolve("macros.json")

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val file = file()
            if (file.exists()) {
                macros.putAll(json.decodeFromString<Map<String, List<String>>>(file.readText()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load macros", e)
        }
    }

    private fun save() {
        try {
            val target = file()
            val temp = File(target.parentFile, target.name + ".tmp")
            temp.writeText(json.encodeToString(macros.toMap()))
            if (!temp.renameTo(target)) {
                target.writeText(temp.readText())
                temp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save macros", e)
        }
    }

    @Synchronized
    fun names(): List<String> {
        ensureLoaded()
        return macros.keys.toList()
    }

    @Synchronized
    fun get(name: String): List<String>? {
        ensureLoaded()
        return macros[name]
    }

    /** Thêm hoặc thay thế macro. Gọi [validate] trước. */
    @Synchronized
    fun put(name: String, steps: List<String>) {
        ensureLoaded()
        macros[name] = steps
        save()
    }

    @Synchronized
    fun remove(name: String): Boolean {
        ensureLoaded()
        if (macros.remove(name) == null) {
            return false
        }
        save()
        return true
    }

    /** Trả về thông báo lỗi nếu danh sách bước không hợp lệ, hoặc null nếu hợp lệ. */
    fun validate(steps: List<String>): String? {
        if (steps.isEmpty()) {
            return "A macro needs at least one step"
        }
        if (steps.size > MAX_STEPS) {
            return "Too many steps (max $MAX_STEPS)"
        }
        for (step in steps) {
            validateStep(step)?.let { return "$it: $step" }
        }
        return null
    }

    private fun findModule(name: String): Module? {
        return ModuleManager.modules.find { !it.private && it.name.equals(name, ignoreCase = true) }
    }

    private fun validateStep(step: String): String? {
        val lower = step.lowercase()
        return when {
            step.startsWith(".") -> when {
                step.length < 2 -> "Empty command"
                lower == ".macro" || lower.startsWith(".macro ") -> "Macros cannot call .macro"
                else -> null
            }

            step.startsWith("/") -> if (step.length < 2) "Empty command" else null

            lower.startsWith("wait ") -> {
                val ms = step.substring(5).trim().toLongOrNull()
                if (ms == null || ms < 0 || ms > MAX_WAIT_MS) "Wait must be 0-$MAX_WAIT_MS ms" else null
            }

            lower.startsWith("on ") || lower.startsWith("off ") -> {
                val moduleName = step.substringAfter(' ').trim()
                if (findModule(moduleName) == null) "Unknown module" else null
            }

            else -> "Unknown step (use on/off <module>, wait <ms>, .command or /command)"
        }
    }

    /** Dừng macro đang chạy. Trả về true nếu có macro bị dừng. */
    @Synchronized
    fun stop(): Boolean {
        val thread = runner ?: return false
        runner = null
        thread.interrupt()
        return thread.isAlive
    }

    /**
     * Chạy macro [name] ở một luồng riêng. Trả về thông báo lỗi nếu không chạy được, hoặc null nếu đã bắt đầu.
     */
    @Synchronized
    fun run(name: String, session: GameSession, commands: CommandHandlerModule): String? {
        ensureLoaded()
        val steps = macros[name] ?: return "Macro not found: $name"
        if (isRunning) {
            return "A macro is already running (use .macro stop)"
        }

        val thread = Thread({
            try {
                for (step in steps) {
                    if (Thread.currentThread().isInterrupted) break
                    execute(step, session, commands)
                }
                if (!Thread.currentThread().isInterrupted) {
                    session.displayClientMessage("§l§b[Macro] §r§7Finished §f$name")
                }
            } catch (_: InterruptedException) {
                // Bị dừng bằng .macro stop hoặc do ngắt kết nối.
            } catch (e: Exception) {
                Log.e(TAG, "Macro $name failed", e)
                session.displayClientMessage("§l§b[Macro] §r§cFailed: ${e.message}")
            } finally {
                synchronized(this) {
                    if (runner === Thread.currentThread()) {
                        runner = null
                    }
                }
            }
        }, "MacroRunner")
        thread.isDaemon = true
        runner = thread
        thread.start()
        return null
    }

    private fun execute(step: String, session: GameSession, commands: CommandHandlerModule) {
        val lower = step.lowercase()
        when {
            step.startsWith(".") -> commands.execute(step)

            step.startsWith("/") -> session.serverBound(CommandRequestPacket().apply {
                command = step
                commandOriginData = CommandOriginData(CommandOriginType.PLAYER, UUID.randomUUID(), "", 0L)
                version = commands.commandRequestVersion
            })

            lower.startsWith("wait ") -> Thread.sleep(step.substring(5).trim().toLong())

            lower.startsWith("on ") || lower.startsWith("off ") -> {
                val module = findModule(step.substringAfter(' ').trim())
                if (module != null) {
                    module.isEnabled = lower.startsWith("on ")
                }
            }
        }
    }

}
