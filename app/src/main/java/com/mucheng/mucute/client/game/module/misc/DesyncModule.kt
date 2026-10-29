package com.mucheng.mucute.client.game.module.misc

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

class DesyncModule : Module("desync", ModuleCategory.Misc) {

    @Volatile
    private var isDesynced = false
    private val storedPackets = ConcurrentLinkedQueue<PlayerAuthInputPacket>()

    // Mỗi lần bật tăng một đơn vị; vòng gửi lại cũ tự dừng nếu thấy số này đã đổi (bật lại hoặc ngắt kết nối).
    @Volatile
    private var generation = 0
    private val updateDelay = 1000L
    private val minResendInterval = 100L
    private val maxResendInterval = 300L

    override fun onEnabled() {
        super.onEnabled()
        generation++
        storedPackets.clear()
        isDesynced = true
    }

    override fun onDisconnect(reason: String) {
        generation++
        isDesynced = false
        storedPackets.clear()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDisabled() {
        super.onDisabled()
        isDesynced = false

        val myGeneration = generation
        val gameSession = session
        GlobalScope.launch {
            delay(updateDelay)
            while (generation == myGeneration) {
                val packet = storedPackets.poll() ?: break
                gameSession.clientBound(packet)
                delay(Random.nextLong(minResendInterval, maxResendInterval))
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isDesynced) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            // Giới hạn bộ nhớ khi bật rất lâu: bỏ gói cũ nhất.
            if (storedPackets.size >= MAX_STORED_PACKETS) {
                storedPackets.poll()
            }
            storedPackets.add(packet)
            interceptablePacket.intercept()
        }
    }

    private companion object {
        const val MAX_STORED_PACKETS = 1200
    }

}
