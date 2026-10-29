package com.mucheng.mucute.client.game.module.misc

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.FriendManager
import com.mucheng.mucute.client.game.MacroManager
import com.mucheng.mucute.client.game.ModuleManager
import com.mucheng.mucute.client.game.module.visual.WaypointModule
import com.mucheng.mucute.client.game.world.Waypoint
import com.mucheng.mucute.client.overlay.MacroButtons
import com.mucheng.mucute.client.game.world.WaypointManager
import kotlin.math.floor
import kotlin.math.sqrt
import org.cloudburstmc.protocol.bedrock.packet.CommandRequestPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

class CommandHandlerModule : Module("command_handler", ModuleCategory.Misc, true, true) {
    private val prefix = "."

    /**
     * Phiên bản trường `version` của CommandRequestPacket, học từ lệnh `/` thật mà game gửi lên
     * để macro gửi lệnh server đúng định dạng. 52 là giá trị dự phòng khi chưa thấy lệnh nào.
     */
    @Volatile
    var commandRequestVersion = 52
        private set

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is CommandRequestPacket) {
            commandRequestVersion = packet.version
            return
        }

        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message
            if (!message.startsWith(prefix)) return

            interceptablePacket.intercept()
            execute(message)
        }
    }

    override fun onDisconnect(reason: String) {
        MacroManager.stop()
    }

    /** Chạy một lệnh client bắt đầu bằng dấu chấm (ví dụ ".fly" hoặc ".waypoint list"). */
    fun execute(message: String) {
        val args = message.substring(prefix.length).split(" ")
        val command = args[0].lowercase()

        when (command) {
            "help" -> {
                displayHelp(args.getOrNull(1))
            }
            "goto" -> {
                val baritoneModule = ModuleManager.modules.find { it is BaritoneModule } as? BaritoneModule
                if (baritoneModule == null) {
                    session.displayClientMessage("§cBaritone module not found")
                    return
                }
                baritoneModule.handleGotoCommand(message)
            }
            "config" -> handleConfig(args)
            "friend", "friends" -> handleFriend(args)
            "macro", "macros" -> handleMacro(args, message)
            "waypoint", "wp" -> handleWaypoint(args)
            "replay" -> {
                val replayModule = ModuleManager.modules.find { it is ReplayModule } as? ReplayModule
                if (replayModule == null) {
                    session.displayClientMessage("§cReplay module not found")
                    return
                }

                when (args.getOrNull(1)?.lowercase()) {
                    "record" -> replayModule.startRecording()
                    "play" -> replayModule.startPlayback()
                    "stop" -> {
                        replayModule.stopRecording()
                        replayModule.stopPlayback()
                    }
                    "save" -> {
                        val name = args.getOrNull(2)
                        if (name == null) {
                            session.displayClientMessage("§cUsage: .replay save <name>")
                            return
                        }
                        replayModule.saveReplay(name)
                    }
                    "load" -> {
                        val name = args.getOrNull(2)
                        if (name == null) {
                            session.displayClientMessage("§cUsage: .replay load <name>")
                            return
                        }
                        replayModule.loadReplay(name)
                    }
                    else -> {
                        session.displayClientMessage("""
                            §l§b[Replay] §r§7Commands:
                            §f.replay record §7- Start recording
                            §f.replay play §7- Play last recording  
                            §f.replay stop §7- Stop recording/playback
                            §f.replay save <name> §7- Save recording
                            §f.replay load <name> §7- Load recording
                        """.trimIndent())
                    }
                }
            }
            else -> {
                val module = ModuleManager.modules.find { it.name.equals(command, ignoreCase = true) }
                if (module != null && !module.private) {
                    module.isEnabled = !module.isEnabled
                } else {
                    session.displayClientMessage("§l§b[MuCuteClient] §r§cModule not found: §f$command")
                }
            }
        }
    }

    private fun message(text: String) = session.displayClientMessage("§l§b[MuCuteClient] §r$text")

    private fun handleConfig(args: List<String>) {
        val action = args.getOrNull(1)?.lowercase()
        val name = args.getOrNull(2)

        if (action == "list") {
            val profiles = ModuleManager.listProfiles()
            if (profiles.isEmpty()) {
                message("§7No profiles saved yet. Use §f.config save <name>")
            } else {
                message("§7Profiles: §f${profiles.joinToString("§7, §f")}")
            }
            return
        }

        if (action !in setOf("save", "load", "delete")) {
            session.displayClientMessage(
                """
                §l§b[Config] §r§7Commands:
                §f.config save <name> §7- Save current settings as a profile
                §f.config load <name> §7- Load a profile
                §f.config delete <name> §7- Delete a profile
                §f.config list §7- List profiles
                """.trimIndent()
            )
            return
        }

        if (name == null || !ModuleManager.isValidProfileName(name)) {
            message("§cProfile name must be 1-32 characters: letters, digits, _ or -")
            return
        }

        try {
            when (action) {
                "save" -> {
                    ModuleManager.saveProfile(name)
                    message("§aSaved profile §f$name")
                }

                "load" -> {
                    if (ModuleManager.loadProfile(name)) {
                        message("§aLoaded profile §f$name")
                    } else {
                        message("§cProfile not found: §f$name")
                    }
                }

                "delete" -> {
                    if (ModuleManager.deleteProfile(name)) {
                        message("§aDeleted profile §f$name")
                    } else {
                        message("§cProfile not found: §f$name")
                    }
                }
            }
        } catch (e: Exception) {
            message("§cConfig error: ${e.message}")
        }
    }

    private fun handleFriend(args: List<String>) {
        val tag = "§l§b[Friend] §r"
        // Tên Bedrock có thể chứa khoảng trắng nên ghép lại phần còn lại của câu lệnh.
        val name = args.drop(2).joinToString(" ").trim()

        when (args.getOrNull(1)?.lowercase()) {
            "add" -> when {
                name.isEmpty() -> session.displayClientMessage("${tag}§cUsage: .friend add <name>")
                FriendManager.add(name) -> session.displayClientMessage("${tag}§aAdded §f$name")
                else -> session.displayClientMessage("${tag}§f$name §cis already a friend")
            }

            "remove", "delete" -> when {
                name.isEmpty() -> session.displayClientMessage("${tag}§cUsage: .friend remove <name>")
                FriendManager.remove(name) -> session.displayClientMessage("${tag}§aRemoved §f$name")
                else -> session.displayClientMessage("${tag}§cNot a friend: §f$name")
            }

            "list" -> {
                val friends = FriendManager.all
                if (friends.isEmpty()) {
                    session.displayClientMessage("${tag}§7No friends yet. Use §f.friend add <name>")
                } else {
                    session.displayClientMessage("${tag}§7Friends (${friends.size}): §f${friends.joinToString("§7, §f")}")
                }
            }

            "clear" -> {
                FriendManager.clear()
                session.displayClientMessage("${tag}§aCleared friends list")
            }

            else -> session.displayClientMessage(
                """
                §l§b[Friend] §r§7Commands:
                §f.friend add <name> §7- Add a friend (auto-attacks skip them)
                §f.friend remove <name> §7- Remove a friend
                §f.friend list §7- List friends
                §f.friend clear §7- Remove all
                """.trimIndent()
            )
        }
    }

    private fun handleMacro(args: List<String>, message: String) {
        val tag = "§l§b[Macro] §r"
        val action = args.getOrNull(1)?.lowercase()
        val name = args.getOrNull(2)

        when (action) {
            "add" -> {
                if (name == null || !MacroManager.isValidName(name)) {
                    session.displayClientMessage("${tag}§cName must be 1-32 characters: letters, digits, _ or -")
                    return
                }
                // ".macro add <name> <các bước>": phần thứ 4 là toàn bộ các bước (có thể chứa khoảng trắng),
                // tách bằng dấu ;. Tách theo vị trí để không nhầm khi tên macro trùng một đoạn của lệnh.
                val rest = message.split(" ", limit = 4).getOrNull(3)?.trim().orEmpty()
                val steps = rest.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                val error = MacroManager.validate(steps)
                if (error != null) {
                    session.displayClientMessage("${tag}§c$error")
                    return
                }
                MacroManager.put(name, steps)
                session.displayClientMessage("${tag}§aSaved §f$name §7(${steps.size} steps)")
            }

            "run" -> {
                if (name == null) {
                    session.displayClientMessage("${tag}§cUsage: .macro run <name>")
                    return
                }
                val error = MacroManager.run(name, session, this)
                if (error != null) {
                    session.displayClientMessage("${tag}§c$error")
                } else {
                    session.displayClientMessage("${tag}§7Running §f$name")
                }
            }

            "stop" -> {
                if (MacroManager.stop()) {
                    session.displayClientMessage("${tag}§cStopped")
                } else {
                    session.displayClientMessage("${tag}§7No macro is running")
                }
            }

            "button" -> {
                if (name == null || MacroManager.get(name) == null) {
                    session.displayClientMessage("${tag}§cUsage: .macro button <name> (macro must exist)")
                } else if (MacroButtons.toggle(name)) {
                    session.displayClientMessage("${tag}§aOn-screen button added for §f$name §7(drag it to move)")
                } else {
                    session.displayClientMessage("${tag}§7Removed the on-screen button for §f$name")
                }
            }

            "remove", "delete" -> {
                if (name == null) {
                    session.displayClientMessage("${tag}§cUsage: .macro remove <name>")
                } else if (MacroManager.remove(name)) {
                    MacroButtons.hide(name)
                    session.displayClientMessage("${tag}§aRemoved §f$name")
                } else {
                    session.displayClientMessage("${tag}§cMacro not found: §f$name")
                }
            }

            "show" -> {
                val steps = name?.let { MacroManager.get(it) }
                if (name == null) {
                    session.displayClientMessage("${tag}§cUsage: .macro show <name>")
                } else if (steps == null) {
                    session.displayClientMessage("${tag}§cMacro not found: §f$name")
                } else {
                    session.displayClientMessage("${tag}§f$name§7:")
                    steps.forEachIndexed { index, step ->
                        session.displayClientMessage("§7${index + 1}. §f$step")
                    }
                }
            }

            "list" -> {
                val names = MacroManager.names()
                if (names.isEmpty()) {
                    session.displayClientMessage("${tag}§7No macros yet. Use §f.macro add <name> <steps>")
                } else {
                    session.displayClientMessage("${tag}§7Macros: §f${names.joinToString("§7, §f")}")
                }
            }

            else -> session.displayClientMessage(
                """
                §l§b[Macro] §r§7Commands:
                §f.macro add <name> <step; step; ...> §7- Save a macro
                §f.macro run <name> §7- Run it
                §f.macro stop §7- Stop the running macro
                §f.macro show <name> §7- Show its steps
                §f.macro button <name> §7- Toggle a one-tap on-screen button
                §f.macro list / remove <name>
                §7Steps: §fon <module>§7, §foff <module>§7, §fwait <ms>§7, §f.command§7, §f/command
                §7Example: §f.macro add pvp on killaura; wait 5000; off killaura
                """.trimIndent()
            )
        }
    }

    private fun dimensionName(dimension: Int) = when (dimension) {
        0 -> "Overworld"
        1 -> "Nether"
        2 -> "End"
        else -> "Dim $dimension"
    }

    private fun handleWaypoint(args: List<String>) {
        val tag = "§l§b[Waypoint] §r"
        val player = session.localPlayer

        when (args.getOrNull(1)?.lowercase()) {
            "add" -> {
                val name = args.getOrNull(2)
                if (name == null || name.length > WaypointManager.MAX_NAME_LENGTH) {
                    session.displayClientMessage("${tag}§cUsage: .waypoint add <name> [x y z]")
                    return
                }

                val x: Int
                val y: Int
                val z: Int
                if (args.size >= 6) {
                    val parsed = args.subList(3, 6).map { it.toDoubleOrNull() }
                    if (parsed.any { it == null }) {
                        session.displayClientMessage("${tag}§cInvalid coordinates")
                        return
                    }
                    x = floor(parsed[0]!!).toInt()
                    y = floor(parsed[1]!!).toInt()
                    z = floor(parsed[2]!!).toInt()
                } else if (args.size == 3) {
                    x = floor(player.posX).toInt()
                    y = floor(player.posY - WaypointModule.PLAYER_EYE_HEIGHT).toInt()
                    z = floor(player.posZ).toInt()
                } else {
                    session.displayClientMessage("${tag}§cUsage: .waypoint add <name> [x y z]")
                    return
                }

                if (WaypointManager.add(Waypoint(name, x, y, z, player.dimension))) {
                    session.displayClientMessage("${tag}§aAdded §f$name §7($x $y $z)")
                } else {
                    session.displayClientMessage("${tag}§cA waypoint named §f$name §calready exists")
                }
            }

            "remove", "delete" -> {
                val name = args.getOrNull(2)
                if (name == null) {
                    session.displayClientMessage("${tag}§cUsage: .waypoint remove <name>")
                } else if (WaypointManager.remove(name)) {
                    session.displayClientMessage("${tag}§aRemoved §f$name")
                } else {
                    session.displayClientMessage("${tag}§cWaypoint not found: §f$name")
                }
            }

            "list" -> {
                val waypoints = WaypointManager.all
                if (waypoints.isEmpty()) {
                    session.displayClientMessage("${tag}§7No waypoints. Use §f.waypoint add <name>")
                    return
                }
                session.displayClientMessage("${tag}§7Waypoints (${waypoints.size}):")
                waypoints.forEach {
                    val where = if (it.dimension == player.dimension) {
                        val dx = it.x + 0.5f - player.posX
                        val dz = it.z + 0.5f - player.posZ
                        "${sqrt(dx * dx + dz * dz).toInt()}m"
                    } else {
                        dimensionName(it.dimension)
                    }
                    session.displayClientMessage("§f${it.name} §7(${it.x} ${it.y} ${it.z}) §8- §7$where")
                }
            }

            "goto" -> {
                val name = args.getOrNull(2)
                val waypoint = name?.let { WaypointManager.find(it) }
                if (name == null) {
                    session.displayClientMessage("${tag}§cUsage: .waypoint goto <name>")
                } else if (waypoint == null) {
                    session.displayClientMessage("${tag}§cWaypoint not found: §f$name")
                } else if (waypoint.dimension != player.dimension) {
                    session.displayClientMessage("${tag}§cThat waypoint is in the ${dimensionName(waypoint.dimension)}")
                } else {
                    val baritone = ModuleManager.modules.find { it is BaritoneModule } as? BaritoneModule
                    if (baritone == null) {
                        session.displayClientMessage("§cBaritone module not found")
                        return
                    }
                    baritone.handleGotoCommand(".goto ${waypoint.x + 0.5} ${waypoint.y} ${waypoint.z + 0.5}")
                }
            }

            "clear" -> {
                WaypointManager.clear()
                session.displayClientMessage("${tag}§aCleared all waypoints")
            }

            else -> {
                session.displayClientMessage(
                    """
                    §l§b[Waypoint] §r§7Commands:
                    §f.waypoint add <name> [x y z] §7- Add (default: here)
                    §f.waypoint remove <name> §7- Remove
                    §f.waypoint list §7- List waypoints
                    §f.waypoint goto <name> §7- Walk there (Baritone)
                    §f.waypoint clear §7- Remove all
                    """.trimIndent()
                )
            }
        }
    }

    private fun displayHelp(category: String?) {
        val header = """
            §l§b[MuCuteClient] §r§7Module List
            §7Commands:
            §f.help <category> §7- View modules in a category
            §f.<module> §7- Toggle a module
            §f.help §7- Show all categories
            §f.config §7- Save/load config profiles
            §f.waypoint §7- Manage waypoints
            §f.friend §7- Manage friends
            §f.macro §7- Run saved command sequences
            §r§7
        """.trimIndent()

        session.displayClientMessage(header)

        if (category != null) {
            try {
                val moduleCategory = ModuleCategory.valueOf(category.uppercase())
                displayCategoryModules(moduleCategory)
            } catch (e: IllegalArgumentException) {
                session.displayClientMessage("§cInvalid category: $category")
                session.displayClientMessage("§7Available categories: ${ModuleCategory.entries.joinToString("§f, §7") { it.name.lowercase() }}")
            }
            return
        }

        ModuleCategory.entries.forEach { cat ->
            displayCategoryModules(cat)
        }
    }

    private fun displayCategoryModules(category: ModuleCategory) {
        val modules = ModuleManager.modules
            .filterNot { it.private }
            .filter { it.category == category }

        if (modules.isEmpty()) return

        session.displayClientMessage("§l§b§m--------------------")
        session.displayClientMessage("§l§b${category.name} Modules:")
        session.displayClientMessage("§r§7")

        modules.chunked(3).forEach { row ->
            val formattedRow = row.joinToString("   ") { module ->
                val status = if (module.isEnabled) "§a✔️" else "§c✘"
                "$status §f${module.name}"
            }
            session.displayClientMessage(formattedRow)
        }

        session.displayClientMessage("§r§7")
    }
}