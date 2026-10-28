package com.mucheng.mucute.client.game.world

import com.mucheng.mucute.client.game.GameSession
import com.mucheng.mucute.client.game.entity.Entity
import com.mucheng.mucute.client.game.entity.EntityUnknown
import com.mucheng.mucute.client.game.entity.Item
import com.mucheng.mucute.client.game.entity.Player
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket
import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket
import org.cloudburstmc.protocol.bedrock.packet.TakeItemEntityPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Suppress("MemberVisibilityCanBePrivate")
class Level(val session: GameSession) {

    val entityMap = ConcurrentHashMap<Long, Entity>()

    val playerMap = ConcurrentHashMap<UUID, PlayerListPacket.Entry>()

    // uniqueEntityId -> runtimeEntityId, để RemoveEntityPacket tra cứu O(1) thay vì duyệt cả entityMap.
    private val runtimeIdByUniqueId = ConcurrentHashMap<Long, Long>()

    fun onDisconnect() {
        clearEntities()
        playerMap.clear()
    }

    private fun clearEntities() {
        entityMap.clear()
        runtimeIdByUniqueId.clear()
    }

    private fun addEntity(entity: Entity) {
        // Nếu runtimeEntityId này đã có entity cũ thì bỏ mapping uniqueId của nó.
        entityMap.put(entity.runtimeEntityId, entity)?.let {
            runtimeIdByUniqueId.remove(it.uniqueEntityId)
        }
        runtimeIdByUniqueId[entity.uniqueEntityId] = entity.runtimeEntityId
    }

    private fun removeEntityByRuntimeId(runtimeEntityId: Long) {
        entityMap.remove(runtimeEntityId)?.let {
            runtimeIdByUniqueId.remove(it.uniqueEntityId)
        }
    }

    fun onPacketBound(packet: BedrockPacket) {
        when (packet) {
            is StartGamePacket -> {
                clearEntities()
                playerMap.clear()
            }

            is AddEntityPacket -> {
                val entity = EntityUnknown(
                    packet.runtimeEntityId,
                    packet.uniqueEntityId,
                    packet.identifier
                ).apply {
                    move(packet.position)
                    rotate(packet.rotation)
                    handleSetData(packet.metadata)
                    handleSetAttribute(packet.attributes)
                }
                addEntity(entity)
            }

            is AddItemEntityPacket -> {
                val entity = Item(packet.runtimeEntityId, packet.uniqueEntityId).apply {
                    move(packet.position)
                    handleSetData(packet.metadata)
                }
                addEntity(entity)
            }

            is AddPlayerPacket -> {
                val entity = Player(
                    packet.runtimeEntityId,
                    packet.uniqueEntityId,
                    packet.uuid,
                    packet.username
                ).apply {
                    move(packet.position)
                    rotate(packet.rotation)
                    handleSetData(packet.metadata)
                }
                addEntity(entity)
            }

            is RemoveEntityPacket -> {
                val runtimeEntityId = runtimeIdByUniqueId[packet.uniqueEntityId] ?: return
                removeEntityByRuntimeId(runtimeEntityId)
            }

            is TakeItemEntityPacket -> {
                removeEntityByRuntimeId(packet.itemRuntimeEntityId)
            }

            is PlayerListPacket -> {
                val add = packet.action == PlayerListPacket.Action.ADD
                packet.entries.forEach {
                    if (add) {
                        playerMap[it.uuid] = it
                    } else {
                        playerMap.remove(it.uuid)
                    }
                }
            }

            else -> dispatchToEntities(packet)
        }
    }

    /**
     * Chỉ chuyển packet tới entity thực sự cần xử lý, thay vì duyệt toàn bộ entityMap cho mọi packet.
     * Entity.onPacketBound và Player.onPacketBound chỉ phản ứng với đúng các loại packet dưới đây
     * (so khớp theo runtimeEntityId, riêng SetEntityLinkPacket so khớp theo uniqueEntityId nên phải
     * gửi cho tất cả). Nếu sau này thêm xử lý packet mới trong Entity thì cần bổ sung ở đây.
     */
    private fun dispatchToEntities(packet: BedrockPacket) {
        when (packet) {
            is MoveEntityAbsolutePacket -> entityMap[packet.runtimeEntityId]?.onPacketBound(packet)
            is MoveEntityDeltaPacket -> entityMap[packet.runtimeEntityId]?.onPacketBound(packet)
            is MovePlayerPacket -> entityMap[packet.runtimeEntityId]?.onPacketBound(packet)
            is SetEntityDataPacket -> entityMap[packet.runtimeEntityId]?.onPacketBound(packet)
            is UpdateAttributesPacket -> entityMap[packet.runtimeEntityId]?.onPacketBound(packet)
            is MobEffectPacket -> entityMap[packet.runtimeEntityId]?.onPacketBound(packet)
            is SetEntityLinkPacket -> entityMap.values.forEach { entity ->
                entity.onPacketBound(packet)
            }

            else -> {}
        }
    }

}
