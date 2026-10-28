package com.mucheng.mucute.client.game.module.effect

import com.mucheng.mucute.client.game.data.Effect

class NightVisionModule : EffectModule("nightVision", Effect.NIGHT_VISION) {

    // Giữ kiểu Int như bản gốc (không trừ 1) để tương thích file cấu hình cũ.
    private val amplifierValue by intValue("amplifier", 1, 1..5)

    override fun amplifier() = amplifierValue

}
