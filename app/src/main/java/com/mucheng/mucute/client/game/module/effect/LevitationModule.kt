package com.mucheng.mucute.client.game.module.effect

import com.mucheng.mucute.client.game.data.Effect

class LevitationModule : EffectModule("levitation", Effect.LEVITATION, effectDuration = 21 * 20) {

    override fun amplifier() = 5

}
