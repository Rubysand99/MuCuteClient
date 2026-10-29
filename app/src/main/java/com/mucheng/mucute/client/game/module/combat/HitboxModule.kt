package com.mucheng.mucute.client.game.module.combat

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.entity.*
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket

class HitboxModule : Module("hitbox", ModuleCategory.Combat) {

    private val hitboxWidth by floatValue("width", 1.5f, 0.5f..12f)
    private val hitboxHeight by floatValue("height", 1.5f, 0.5f..12f)
    private var playersOnly by boolValue("players_only", true)
    private var mobsOnly by boolValue("include_mobs", false)
    private val visualizeHitbox by boolValue("visualize", true)
    private var lastParticleTime = 0L
    private val particleInterval = 500L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()

            if (session.localPlayer.tickExists % 40 == 0L) {
                // Hạt chỉ hiện cho một thực thể mỗi 500ms, quyết định một lần cho cả lượt quét.
                var showParticles = visualizeHitbox && currentTime - lastParticleTime >= particleInterval
                session.level.entityMap.values.forEach { entity ->
                    if (entity.isTarget()) {
                        val metadata = EntityDataMap()
                        metadata.put(EntityDataTypes.WIDTH, hitboxWidth)
                        metadata.put(EntityDataTypes.HEIGHT, hitboxHeight)
                        metadata.put(EntityDataTypes.SCALE, 1.0f)

                        session.clientBound(SetEntityDataPacket().apply {
                            runtimeEntityId = entity.runtimeEntityId
                            this.metadata = metadata
                        })

                        if (showParticles) {
                            showParticles = false
                            lastParticleTime = currentTime
                            visualizeHitboxWithParticles(entity)
                        }
                    }
                }
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            session.level.entityMap.values.forEach { entity ->
                if (entity.isTarget()) {
                    val metadata = EntityDataMap()
                    metadata.put(EntityDataTypes.WIDTH, 0.6f)
                    metadata.put(EntityDataTypes.HEIGHT, 1.8f)
                    metadata.put(EntityDataTypes.SCALE, 1.0f)

                    session.clientBound(SetEntityDataPacket().apply {
                        runtimeEntityId = entity.runtimeEntityId
                        this.metadata = metadata
                    })
                }
            }
        }
    }

    private fun visualizeHitboxWithParticles(entity: Entity) {
        // Trước đây gửi cùng một gói 'particleCount' lần mà kết quả giống hệt nhau; một gói là đủ.
        session.clientBound(EntityEventPacket().apply {
            runtimeEntityId = entity.runtimeEntityId
            type = EntityEventType.LOVE_PARTICLES
            data = 0
        })
    }

    private fun Entity.isTarget() = EntityTargets.isTarget(session, this, playersOnly, mobsOnly)
}
