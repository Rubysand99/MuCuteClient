package com.mucheng.mucute.client.game

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /** Tên macro đang chạy (null nếu không có); là state của Compose nên nút trên màn hình tự đổi màu. */
    var runningName by mutableStateOf<String?>(null)
        private set

    val isRunning: Boolean
        get() = runner?.isAlive == true

    // ---- Nút chạy macro trên màn hình: tên -> vị trí (x, y) ----
    private val buttons = TreeMap<String, List<Int>>(String.CASE_INSENSITIVE_ORDER)

    private var buttonsLoaded = false

    private fun buttonsFile(): File = AppContext.instance.filesDir.resolve("macro_buttons.json")

    @Synchronized
    private fun ensureButtonsLoaded() {
        if (buttonsLoaded) return
        buttonsLoaded = true
        try {
            val file = buttonsFile()
            if (file.exists()) {
                buttons.putAll(json.decodeFromString<Map<String, List<Int>>>(file.readText()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load macro buttons", e)
        }
    }

    private fun saveButtons() {
        try {
            val target = buttonsFile()
            val temp = File(target.parentFile, target.name + ".tmp")
            temp.writeText(json.encodeToString(buttons.toMap()))
            if (!temp.renameTo(target)) {
                target.writeText(temp.readText())
                temp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save macro buttons", e)
        }
    }

    /** Tên các macro có nút trên màn hình (bỏ qua nút của macro đã bị xoá). */
    @Synchronized
    fun buttonNames(): List<String> {
        ensureLoaded()
        ensureButtonsLoaded()
        return buttons.keys.filter { macros.containsKey(it) }
    }

    @Synchronized
    fun hasButton(name: String): Boolean {
        ensureButtonsLoaded()
        return buttons.containsKey(name)
    }

    /** Vị trí đã lưu của nút, hoặc null nếu chưa có. */
    @Synchronized
    fun buttonPosition(name: String): Pair<Int, Int>? {
        ensureButtonsLoaded()
        val position = buttons[name] ?: return null
        return position[0] to position[1]
    }

    /**
     * Đăng ký nút cho macro [name] với vị trí mặc định nếu chưa có. Các nút xếp dọc theo cạnh phải
     * (nút của module xếp từ cạnh trái), sang cột mới khi hết chiều cao màn hình.
     */
    @Synchronized
    fun addButton(name: String) {
        ensureButtonsLoaded()
        if (buttons.containsKey(name)) return

        val metrics = AppContext.instance.resources.displayMetrics
        val density = metrics.density
        val top = (72 * density).toInt()
        val rowStep = (64 * density).toInt()
        val columnStep = (150 * density).toInt()
        val rowsPerColumn = ((metrics.heightPixels - top - rowStep) / rowStep).coerceAtLeast(1)

        val index = buttons.size
        val x = (metrics.widthPixels - columnStep * (1 + index / rowsPerColumn)).coerceAtLeast(0)
        val y = top + (index % rowsPerColumn) * rowStep
        buttons[name] = listOf(x, y)
        saveButtons()
    }

    @Synchronized
    fun setButtonPosition(name: String, x: Int, y: Int) {
        ensureButtonsLoaded()
        buttons[name] = listOf(x, y)
        saveButtons()
    }

    @Synchronized
    fun removeButton(name: String) {
        ensureButtonsLoaded()
        if (buttons.remove(name) != null) {
            saveButtons()
        }
    }

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
        removeButton(name)
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
        runningName = null
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
                        runningName = null
                    }
                }
            }
        }, "MacroRunner")
        thread.isDaemon = true
        runner = thread
        runningName = name
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

    /**
     * Dùng cho nút trên màn hình: chạm khi macro này đang chạy thì dừng, ngược lại thì chạy.
     * Cần có phiên game (đang kết nối server); kết quả báo trong khung chat của game.
     */
    fun trigger(name: String) {
        val commands = ModuleManager.modules.firstOrNull { it is CommandHandlerModule } as? CommandHandlerModule
        if (commands == null || !commands.isSessionCreated) {
            return
        }
        val session = commands.session

        if (runningName == name) {
            stop()
            session.displayClientMessage("§l§b[Macro] §r§cStopped")
            return
        }

        val error = run(name, session, commands)
        if (error != null) {
            session.displayClientMessage("§l§b[Macro] §r§c$error")
        }
    }

}
