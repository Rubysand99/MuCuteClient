package com.mucheng.mucute.client.game

import android.util.Log
import com.mucheng.mucute.client.application.AppContext
import com.mucheng.mucute.client.game.entity.LocalPlayer
import com.mucheng.mucute.client.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(val muCuteRelaySession: MuCuteRelaySession) : ComposedPacketHandler {

    private companion object {
        const val TAG = "GameSession"
    }

    val localPlayer = LocalPlayer(this)

    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContext.instance.packageManager.getPackageInfo(
            AppContext.instance.packageName, 0
        ).versionName
    }

    fun clientBound(packet: BedrockPacket) {
        muCuteRelaySession.clientBound(packet)
    }

    fun serverBound(packet: BedrockPacket) {
        muCuteRelaySession.serverBound(packet)
    }

    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        // Lỗi trong code xử lý (module, Level...) không được làm đứt kết nối: ghi log rồi cho packet đi tiếp.
        try {
            localPlayer.onPacketBound(packet)
            level.onPacketBound(packet)
        } catch (e: Throwable) {
            Log.e(TAG, "State tracking failed on ${packet.javaClass.simpleName}", e)
        }

        val interceptablePacket = InterceptablePacket(packet)

        for (module in ModuleManager.activeModules) {
            try {
                module.beforePacketBound(interceptablePacket)
            } catch (e: Throwable) {
                Log.e(TAG, "Module ${module.name} failed on ${packet.javaClass.simpleName}", e)
            }
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

        return false
    }

    override fun afterPacketBound(packet: BedrockPacket) {
        for (module in ModuleManager.activeModules) {
            try {
                module.afterPacketBound(packet)
            } catch (e: Throwable) {
                Log.e(TAG, "Module ${module.name} failed after ${packet.javaClass.simpleName}", e)
            }
        }
    }

    override fun onDisconnect(reason: String) {
        // Ghi lý do ngắt kết nối (logcat + toast) để biết là do đăng nhập, phiên bản hay server từ chối.
        android.util.Log.e("GameSession", "Disconnected: $reason")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                com.mucheng.mucute.client.application.AppContext.instance,
                "Disconnected: $reason",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        localPlayer.onDisconnect()
        level.onDisconnect()

        for (module in ModuleManager.modules) {
            module.onDisconnect(reason)
        }
    }

    fun displayClientMessage(message: String, type: TextPacket.Type = TextPacket.Type.RAW) {
        val textPacket = TextPacket()
        textPacket.type = type
        textPacket.isNeedsTranslation = false
        textPacket.sourceName = ""
        textPacket.message = message
        textPacket.xuid = ""
        textPacket.platformChatId = ""
        textPacket.filteredMessage = ""
        clientBound(textPacket)
    }

}