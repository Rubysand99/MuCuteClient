package com.mucheng.mucute.client.game.module.combat

import com.mucheng.mucute.client.game.GameSession
import com.mucheng.mucute.client.game.entity.Entity
import com.mucheng.mucute.client.game.entity.EntityUnknown
import com.mucheng.mucute.client.game.entity.LocalPlayer
import com.mucheng.mucute.client.game.entity.MobList
import com.mucheng.mucute.client.game.entity.Player

/** Bộ lọc mục tiêu dùng chung cho các module combat (trước đây mỗi module chép lại một bản). */
internal object EntityTargets {

    /** Người chơi "ảo" (không có trong danh sách người chơi hoặc tên rỗng) bị coi là bot. */
    fun isBot(session: GameSession, player: Player): Boolean {
        if (player is LocalPlayer) return false
        val entry = session.level.playerMap[player.uuid] ?: return true
        return entry.name.isBlank()
    }

    fun isTarget(session: GameSession, entity: Entity, playersOnly: Boolean, mobsOnly: Boolean): Boolean {
        return when (entity) {
            is LocalPlayer -> false
            is Player -> !mobsOnly && !isBot(session, entity)
            is EntityUnknown -> when {
                mobsOnly -> entity.identifier in MobList.mobTypes
                playersOnly -> false
                else -> true
            }
            else -> false
        }
    }
}
