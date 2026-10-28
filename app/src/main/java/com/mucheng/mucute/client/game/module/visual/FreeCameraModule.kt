package com.mucheng.mucute.client.game.module.visual

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket

class FreeCameraModule : Module("free_camera", ModuleCategory.Visual) {

    private var originalPosition: Vector3f? = null
    private val flySpeed by floatValue("speed", 0.15f, 0.1f..1.5f) // Add configurable speed

    private val enableFlyNoClipPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(
                arrayOf(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.MAY_FLY,
                    Ability.FLY_SPEED,
                    Ability.WALK_SPEED,
                    Ability.NO_CLIP,
                    Ability.OPERATOR_COMMANDS
                )
            )
            walkSpeed = 0.1f
            flySpeed = 0.15f
        })
    }

    private val disableFlyNoClipPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(
                arrayOf(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.OPERATOR_COMMANDS
                )
            )
            walkSpeed = 0.1f
        })
    }

    private var isFlyNoClipEnabled = false

    override fun onDisconnect(reason: String) {
        // Phiên mới chưa có ability fly/no-clip nên coi như chưa bật.
        isFlyNoClipEnabled = false
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            // Store original position immediately when enabled
            originalPosition = Vector3f.from(
                session.localPlayer.posX,
                session.localPlayer.posY,
                session.localPlayer.posZ
            )

            GlobalScope.launch {
                for (i in 5 downTo 1) {
                    // Người dùng tắt module trong lúc đếm ngược thì dừng, không bật fly nữa.
                    if (!isEnabled) return@launch
                    val countdownMessage = "§l§b[MuCute] §r§7FreeCam will enable in §e$i §7seconds"
                    sendCountdownMessage(countdownMessage)
                    delay(1000)
                }
                if (!isEnabled) return@launch

                enableFlyNoClipPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
                session.clientBound(enableFlyNoClipPacket)
                isFlyNoClipEnabled = true
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated && originalPosition != null) {
            // Return to original position when disabled.
            // (Trước đây gửi SetEntityMotionPacket với toạ độ làm vận tốc nên không về đúng chỗ.)
            val movePacket = MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                position = originalPosition
                rotation = session.localPlayer.vec3Rotation
                mode = MovePlayerPacket.Mode.NORMAL
                isOnGround = false
                ridingRuntimeEntityId = 0
                tick = session.localPlayer.tickExists
            }
            session.clientBound(movePacket)
            originalPosition = null

            disableFlyNoClipPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            session.clientBound(disableFlyNoClipPacket)
            isFlyNoClipEnabled = false
        }
    }

    private fun sendCountdownMessage(message: String) {
        val textPacket = TextPacket().apply {
            type = TextPacket.Type.RAW
            isNeedsTranslation = false
            this.message = message
            xuid = ""
            sourceName = ""
        }

        session.clientBound(textPacket)
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket && isEnabled) {
            // Handle vertical movement
            if (isFlyNoClipEnabled) {
                var verticalMotion = 0f

                // Space for up, Shift for down
                if (packet.inputData.contains(PlayerAuthInputData.JUMPING)) {
                    verticalMotion = flySpeed
                } else if (packet.inputData.contains(PlayerAuthInputData.SNEAKING)) {
                    verticalMotion = -flySpeed
                }

                if (verticalMotion != 0f) {
                    val motionPacket = SetEntityMotionPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        motion = Vector3f.from(0f, verticalMotion, 0f)
                    }
                    session.clientBound(motionPacket)
                }
            }

            interceptablePacket.intercept()
        }
    }
}