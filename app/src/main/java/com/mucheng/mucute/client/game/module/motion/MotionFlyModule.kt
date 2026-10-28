package com.mucheng.mucute.client.game.module.motion

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket

class MotionFlyModule : Module("motion_fly", ModuleCategory.Motion) {

    private val verticalSpeedUp = floatValue("verticalUpSpeed", 11.77f, 1.0f..20.0f)
    private val verticalSpeedDown = floatValue("verticalDownSpeed", 16.55f, 1.0f..20.0f)
    private val motionInterval = floatValue("delay", 472.17f, 100.0f..600.0f)
    private var lastMotionTime = 0L
    private var jitterState = false
    @Volatile
    private var canFly = false

    private val flyAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(arrayOf(
                Ability.BUILD,
                Ability.MINE,
                Ability.DOORS_AND_SWITCHES,
                Ability.OPEN_CONTAINERS,
                Ability.ATTACK_PLAYERS,
                Ability.ATTACK_MOBS,
                Ability.MAY_FLY,
                Ability.FLY_SPEED,
                Ability.WALK_SPEED,
                Ability.OPERATOR_COMMANDS
            ))
            walkSpeed = 0.1f
            flySpeed = 2.19f
        })
    }

    private val resetAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(arrayOf(
                Ability.BUILD,
                Ability.MINE,
                Ability.DOORS_AND_SWITCHES,
                Ability.OPEN_CONTAINERS,
                Ability.ATTACK_PLAYERS,
                Ability.ATTACK_MOBS,
                Ability.OPERATOR_COMMANDS
            ))
            walkSpeed = 0.1f
            flySpeed = 0f
        })
    }

    override fun onDisabled() {
        super.onDisabled()
        // Module tắt không còn nhận packet nên gửi gói reset ability ngay tại đây.
        if (isSessionCreated && canFly) {
            resetAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            session.clientBound(resetAbilitiesPacket)
        }
        canFly = false
    }

    override fun onDisconnect(reason: String) {
        // Phiên mới chưa có ability fly nên phải gửi lại.
        canFly = false
    }

    private fun enableFlyAbilities() {
        if (!canFly) {
            flyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            session.clientBound(flyAbilitiesPacket)
            canFly = true
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket && isEnabled) {
            enableFlyAbilities()
            if (System.currentTimeMillis() - lastMotionTime >= motionInterval.value) {
                val vertical = when {
                    packet.inputData.contains(PlayerAuthInputData.WANT_UP) -> verticalSpeedUp.value
                    packet.inputData.contains(PlayerAuthInputData.WANT_DOWN) -> -verticalSpeedDown.value
                    else -> 0f
                }
                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    motion = Vector3f.from(0f, vertical + (if (jitterState) 0.1f else -0.1f), 0f)
                }
                session.clientBound(motionPacket)
                jitterState = !jitterState
                lastMotionTime = System.currentTimeMillis()
            }
        }
    }
}