package com.mucheng.mucute.client.game.module.combat

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.entity.Entity
import com.mucheng.mucute.client.game.entity.LocalPlayer
import com.mucheng.mucute.client.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import kotlin.math.cos
import kotlin.math.sin

class HitAndRunModule : Module("hitandrun", ModuleCategory.Combat) {

    private var range by floatValue("range", 4.0f, 2f..6f)
    private var hitSpeed by intValue("hit_speed", 200, 100..1000)
    private var jumpHeight by floatValue("jump_height", 0.42f, 0.1f..1f)
    private var circleRadius by floatValue("circle_radius", 1.5f, 0.5f..3f)
    private var playersOnly by boolValue("players_only", true)

    private var lastHitTime = 0L
    private var comboAngle = 0f

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastHitTime >= hitSpeed) {
                val local = session.localPlayer
                val rangeSq = range * range
                val target = session.level.entityMap.values.firstOrNull {
                    it !== local &&
                            it.distanceSq(local) <= rangeSq &&
                            EntityTargets.isTarget(session, it, playersOnly, false)
                }
                if (target != null) {
                    executeCombo(target)
                    lastHitTime = currentTime
                }
            }
        }
    }

    private fun executeCombo(target: Entity) {
        comboAngle += 45f
        if (comboAngle >= 360f) comboAngle = 0f

        val offsetX = circleRadius * cos(Math.toRadians(comboAngle.toDouble())).toFloat()
        val offsetZ = circleRadius * sin(Math.toRadians(comboAngle.toDouble())).toFloat()

        session.clientBound(SetEntityMotionPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            motion = Vector3f.from(offsetX, jumpHeight, offsetZ)
        })

        session.localPlayer.attack(target)
    }
}