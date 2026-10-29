package com.mucheng.mucute.client.game.module.combat

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.entity.*
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class TriggerBotModule : Module("trigger_bot", ModuleCategory.Combat) {

    private var cpsValue by intValue("cps", 12, 1..20)
    private var playersOnly by boolValue("players_only", true)
    private var mobsOnly by boolValue("mobs_only", false)
    private var rangeValue by floatValue("range", 4.0f, 2f..6f)
    private var lastAttackTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()
            val minAttackDelay = 1000L / cpsValue

            if ((currentTime - lastAttackTime) >= minAttackDelay) {
                val local = session.localPlayer
                val rangeSq = rangeValue * rangeValue

                // Hướng nhìn chỉ tính một lần cho cả vòng lặp.
                val rot = local.vec3Rotation
                val yawRad = Math.toRadians(rot.y.toDouble() + 90.0)
                val pitchRad = Math.toRadians(-rot.x.toDouble())
                val lookX = Math.cos(pitchRad) * Math.cos(yawRad)
                val lookY = Math.sin(pitchRad)
                val lookZ = Math.cos(pitchRad) * Math.sin(yawRad)

                for (entity in session.level.entityMap.values) {
                    if (entity.distanceSq(local) > rangeSq) continue
                    if (!EntityTargets.isTarget(session, entity, playersOnly, mobsOnly)) continue
                    if (!isLookingAt(local.vec3Position, entity, lookX, lookY, lookZ)) continue

                    local.attack(entity)
                    lastAttackTime = currentTime
                    break
                }
            }
        }
    }

    private fun isLookingAt(
        playerPos: Vector3f,
        entity: Entity,
        lookX: Double,
        lookY: Double,
        lookZ: Double
    ): Boolean {
        val entityPos = entity.vec3Position

        // Kiểm tra chân, giữa người và đầu.
        for (offset in BODY_OFFSETS) {
            val dx = entityPos.x.toDouble() - playerPos.x.toDouble()
            val dy = entityPos.y.toDouble() + offset - playerPos.y.toDouble()
            val dz = entityPos.z.toDouble() - playerPos.z.toDouble()

            val distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
            if (distance < 1e-6) return true

            val dot = ((dx * lookX + dy * lookY + dz * lookZ) / distance).coerceIn(-1.0, 1.0)
            if (Math.toDegrees(Math.acos(dot)) <= MAX_ANGLE_DEGREES) {
                return true
            }
        }

        return false
    }

    private companion object {
        val BODY_OFFSETS = doubleArrayOf(0.0, 0.9, 1.8)
        const val MAX_ANGLE_DEGREES = 20.0
    }
}
