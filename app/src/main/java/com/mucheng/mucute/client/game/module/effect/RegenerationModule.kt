package com.mucheng.mucute.client.game.module.effect

import com.mucheng.mucute.client.game.data.Effect

class RegenerationModule : EffectModule("regeneration", Effect.REGENERATION) {

    // Giữ nguyên hành vi bản gốc: amplifier gửi đi là -1 (trước đây viết `amplifier -= 1`).
    override fun amplifier() = -1

}
