package com.mucheng.mucute.client.game.module.motion

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.ModuleManager
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class SprintModule : Module("sprint", ModuleCategory.Motion) {

    // Sau khi tắt chỉ cần thêm đúng một lần STOP_SPRINTING để server ngừng trạng thái chạy nhanh.
    // (Trước đây module tắt vẫn gắn STOP_SPRINTING vào mọi PlayerAuthInput, làm hỏng cả
    // việc chạy nhanh bình thường của người chơi.)
    @Volatile
    private var stopPending = false

    // Giữ module trong danh sách xử lý packet cho tới khi gửi xong STOP_SPRINTING.
    override val receivePacketsWhenDisabled: Boolean
        get() = stopPending

    override fun onEnabled() {
        super.onEnabled()
        stopPending = false
    }

    override fun onDisabled() {
        super.onDisabled()
        stopPending = isSessionCreated
        ModuleManager.refreshActiveModules()
    }

    override fun onDisconnect(reason: String) {
        stopPending = false
        ModuleManager.refreshActiveModules()
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) {
            return
        }

        if (isEnabled) {
            packet.inputData.add(PlayerAuthInputData.SPRINTING)
            packet.inputData.add(PlayerAuthInputData.START_SPRINTING)
        } else if (stopPending) {
            packet.inputData.add(PlayerAuthInputData.STOP_SPRINTING)
            stopPending = false
            ModuleManager.refreshActiveModules()
        }
    }
}
