package com.mucheng.mucute.client.game.module.combat

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import com.mucheng.mucute.client.game.entity.*
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class KillauraModule : Module("killaura", ModuleCategory.Combat) {

    private var playersOnly by boolValue("players_only", true)
    private var mobsOnly by boolValue("mobs_only", false)
    private var tpAuraEnabled by boolValue("tp_aura", false)

    private var rangeValue by floatValue("range", 3.7f, 2f..7f)
    // delay = số tick tối thiểu giữa hai lượt; 1 nghĩa là mọi tick đều được phép, tốc độ do cps quyết định.
    private var attackInterval by intValue("delay", 1, 1..20)
    private var cpsValue by intValue("cps", 12, 1..40)
    private var maxTargets by intValue("max_targets", 1, 1..5)
    private var lowestHealthFirst by boolValue("lowest_health_first", false)
    private var boost by intValue("packets", 1, 1..10)
    private var tpspeed by intValue("tp_speed", 1000, 100..2000)

    private var distanceToKeep by floatValue("keep_distance", 2.0f, 1f..5f)

    private var lastTickTime = 0L
    private var tpCooldown = 0L

    // Xô token: mỗi tick cộng thêm cps * thời gian trôi qua, mỗi đòn tốn 1 token. Nhờ vậy cps được giữ đúng
    // trung bình (không bị làm tròn theo nhịp tick) và cps > 20 vẫn đạt được bằng nhiều đòn mỗi tick.
    private var tokens = 0f

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()
            val elapsed = if (lastTickTime == 0L) 0L else (currentTime - lastTickTime).coerceIn(0L, 250L)
            lastTickTime = currentTime
            tokens = (tokens + cpsValue * elapsed / 1000f).coerceAtMost(MAX_HITS_PER_TICK.toFloat())

            if (packet.tick % attackInterval != 0L) return

            val hits = tokens.toInt().coerceAtMost(MAX_HITS_PER_TICK)
            if (hits <= 0) return

            val closestEntities = searchForClosestEntities()
            if (closestEntities.isEmpty()) {
                // Không có mục tiêu thì không dồn token để khỏi tung một loạt đòn khi mục tiêu xuất hiện.
                tokens = tokens.coerceAtMost(1f)
                return
            }

            val count = minOf(maxTargets, closestEntities.size)
            for (i in 0 until count) {
                val entity = closestEntities[i]
                // Dịch chuyển chỉ với mục tiêu đầu tiên (gần nhất / ưu tiên nhất).
                if (i == 0 && tpAuraEnabled && (currentTime - tpCooldown) >= tpspeed) {
                    teleportTo(entity, distanceToKeep)
                    tpCooldown = currentTime
                }

                repeat(hits * boost) {
                    session.localPlayer.attack(entity)
                }
            }

            tokens -= hits
        }
    }

    private fun teleportTo(entity: Entity, distance: Float) {
        val targetPosition = entity.vec3Position
        val playerPosition = session.localPlayer.vec3Position

        val direction = Vector3f.from(
            targetPosition.x - playerPosition.x,
            0f,  // No modification to Y-axis
            targetPosition.z - playerPosition.z
        )

        val length = direction.length()
        val normalizedDirection = if (length != 0f) {
            Vector3f.from(direction.x / length, 0f, direction.z / length)
        } else {
            direction
        }

        val newPosition = Vector3f.from(
            targetPosition.x - normalizedDirection.x * distance,
            playerPosition.y, 
            targetPosition.z - normalizedDirection.z * distance
        )

        val movePlayerPacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = newPosition
            rotation = entity.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = false
            ridingRuntimeEntityId = 0
            tick = session.localPlayer.tickExists
        }

        session.clientBound(movePlayerPacket)
    }



    private val targets = ArrayList<Entity>()

    private val byDistance = Comparator<Entity> { a, b ->
        a.distanceSq(session.localPlayer).compareTo(b.distanceSq(session.localPlayer))
    }

    private val byHealth = Comparator<Entity> { a, b ->
        val healthA = a.attributes["minecraft:health"]?.value ?: Float.MAX_VALUE
        val healthB = b.attributes["minecraft:health"]?.value ?: Float.MAX_VALUE
        val diff = healthA.compareTo(healthB)
        if (diff != 0) diff else byDistance.compare(a, b)
    }

    /**
     * Các mục tiêu trong tầm, đã sắp xếp: gần nhất trước, hoặc ít máu nhất trước (nếu server gửi máu;
     * thực thể không rõ máu xếp sau). Dùng lại một danh sách giữa các tick; chỉ chạy trên luồng xử lý packet.
     */
    private fun searchForClosestEntities(): List<Entity> {
        val local = session.localPlayer
        val rangeSq = rangeValue * rangeValue
        targets.clear()
        for (entity in session.level.entityMap.values) {
            if (entity.distanceSq(local) < rangeSq &&
                EntityTargets.isTarget(session, entity, playersOnly, mobsOnly)
            ) {
                targets.add(entity)
            }
        }
        if (targets.size > 1) {
            targets.sortWith(if (lowestHealthFirst) byHealth else byDistance)
        }
        return targets
    }

    private companion object {
        const val MAX_HITS_PER_TICK = 3
    }
}
