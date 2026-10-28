package com.mucheng.mucute.client.game.module.effect

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

/**
 * Lớp nền cho các module hiệu ứng chỉ có tác dụng phía client:
 * cứ mỗi 20 tick gửi MobEffectPacket ADD cho người chơi, khi tắt thì gửi REMOVE.
 */
abstract class EffectModule(
    name: String,
    private val effectId: Int,
    private val effectDuration: Int = PERSISTENT_DURATION
) : Module(name, ModuleCategory.Effect) {

    /** Giá trị amplifier gửi đi (level hiển thị trừ 1). */
    protected abstract fun amplifier(): Int

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            session.clientBound(MobEffectPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                event = MobEffectPacket.Event.REMOVE
                effectId = this@EffectModule.effectId
            })
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        if (interceptablePacket.packet !is PlayerAuthInputPacket) {
            return
        }

        if (session.localPlayer.tickExists % 20 != 0L) {
            return
        }

        session.clientBound(MobEffectPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            event = MobEffectPacket.Event.ADD
            effectId = this@EffectModule.effectId
            amplifier = this@EffectModule.amplifier()
            isParticles = false
            duration = effectDuration
        })
    }

    companion object {
        const val PERSISTENT_DURATION = 360000
    }

}

/**
 * Module hiệu ứng có thanh chỉnh "amplifier" (1..5) trong cấu hình.
 */
abstract class ConfigurableEffectModule(
    name: String,
    effectId: Int
) : EffectModule(name, effectId) {

    private val amplifierValue by floatValue("amplifier", 1f, 1f..5f)

    override fun amplifier() = amplifierValue.toInt() - 1

}
