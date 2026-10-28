package com.mucheng.mucute.client.game


import android.content.Context
import android.net.Uri
import android.util.Log
import com.mucheng.mucute.client.application.AppContext
import com.mucheng.mucute.client.game.module.combat.AntiCrystalModule
import com.mucheng.mucute.client.game.module.combat.AntiKnockbackModule
import com.mucheng.mucute.client.game.module.combat.CrystalSmashModule
import com.mucheng.mucute.client.game.module.combat.HitAndRunModule
import com.mucheng.mucute.client.game.module.combat.HitboxModule
import com.mucheng.mucute.client.game.module.combat.KillauraModule
import com.mucheng.mucute.client.game.module.combat.TriggerBotModule
import com.mucheng.mucute.client.game.module.effect.AbsorptionModule
import com.mucheng.mucute.client.game.module.effect.BadOmenModule
import com.mucheng.mucute.client.game.module.effect.BlindnessModule
import com.mucheng.mucute.client.game.module.effect.ConduitPowerModule
import com.mucheng.mucute.client.game.module.effect.DarknessModule
import com.mucheng.mucute.client.game.module.effect.FatalPoisonModule
import com.mucheng.mucute.client.game.module.effect.FireResistanceModule
import com.mucheng.mucute.client.game.module.effect.HasteModule
import com.mucheng.mucute.client.game.module.effect.HealthBoostModule
import com.mucheng.mucute.client.game.module.effect.HungerModule
import com.mucheng.mucute.client.game.module.effect.InstantDamageModule
import com.mucheng.mucute.client.game.module.effect.InstantHealthModule
import com.mucheng.mucute.client.game.module.effect.InvisibilityModule
import com.mucheng.mucute.client.game.module.effect.JumpBoostModule
import com.mucheng.mucute.client.game.module.effect.LevitationModule
import com.mucheng.mucute.client.game.module.effect.MiningFatigueModule
import com.mucheng.mucute.client.game.module.effect.NauseaModule
import com.mucheng.mucute.client.game.module.effect.NightVisionModule
import com.mucheng.mucute.client.game.module.effect.PoisonModule
import com.mucheng.mucute.client.game.module.effect.PoseidonModule
import com.mucheng.mucute.client.game.module.effect.RegenerationModule
import com.mucheng.mucute.client.game.module.effect.ResistanceModule
import com.mucheng.mucute.client.game.module.effect.SaturationModule
import com.mucheng.mucute.client.game.module.effect.SlowFallingModule
import com.mucheng.mucute.client.game.module.effect.StrengthModule
import com.mucheng.mucute.client.game.module.effect.SwiftnessModule
import com.mucheng.mucute.client.game.module.effect.VillageHeroModule
import com.mucheng.mucute.client.game.module.effect.WeaknessModule
import com.mucheng.mucute.client.game.module.effect.WitherModule
import com.mucheng.mucute.client.game.module.misc.BaritoneModule
import com.mucheng.mucute.client.game.module.misc.CommandHandlerModule
import com.mucheng.mucute.client.game.module.misc.DesyncModule
import com.mucheng.mucute.client.game.module.misc.FakeDeathModule
import com.mucheng.mucute.client.game.module.misc.FakeXPModule
import com.mucheng.mucute.client.game.module.misc.NoChatModule
import com.mucheng.mucute.client.game.module.motion.NoClipModule
import com.mucheng.mucute.client.game.module.misc.PositionLoggerModule
import com.mucheng.mucute.client.game.module.misc.ReplayModule
import com.mucheng.mucute.client.game.module.visual.TimeShiftModule
import com.mucheng.mucute.client.game.module.visual.WeatherControllerModule
import com.mucheng.mucute.client.game.module.motion.AirJumpModule
import com.mucheng.mucute.client.game.module.motion.AntiAFKModule
import com.mucheng.mucute.client.game.module.motion.AutoWalkModule
import com.mucheng.mucute.client.game.module.motion.BhopModule
import com.mucheng.mucute.client.game.module.motion.FlyModule
import com.mucheng.mucute.client.game.module.motion.HighJumpModule
import com.mucheng.mucute.client.game.module.motion.JetPackModule
import com.mucheng.mucute.client.game.module.motion.MotionFlyModule
import com.mucheng.mucute.client.game.module.motion.SpeedModule
import com.mucheng.mucute.client.game.module.motion.SprintModule
import com.mucheng.mucute.client.game.module.particle.BreezeWindExplosionParticleModule
import com.mucheng.mucute.client.game.module.particle.BubbleParticleModule
import com.mucheng.mucute.client.game.module.particle.DustParticleModule
import com.mucheng.mucute.client.game.module.particle.ExplosionParticleModule
import com.mucheng.mucute.client.game.module.particle.EyeOfEnderDeathParticleModule
import com.mucheng.mucute.client.game.module.particle.FizzParticleModule
import com.mucheng.mucute.client.game.module.particle.HeartParticleModule
import com.mucheng.mucute.client.game.module.visual.BreadcrumbsModule
import com.mucheng.mucute.client.game.module.visual.ESPModule
import com.mucheng.mucute.client.game.module.visual.FreeCameraModule
import com.mucheng.mucute.client.game.module.visual.HudModule
import com.mucheng.mucute.client.game.module.visual.NetworkInfoModule
import com.mucheng.mucute.client.game.module.visual.NoHurtCameraModule
import com.mucheng.mucute.client.game.module.visual.PositionDisplayModule
import com.mucheng.mucute.client.game.module.visual.SpeedDisplayModule
import com.mucheng.mucute.client.game.module.visual.WaypointModule
import com.mucheng.mucute.client.game.module.visual.WorldStateModule
import com.mucheng.mucute.client.game.module.visual.ZoomModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

object ModuleManager {

    private val _modules: MutableList<Module> = ArrayList()

    val modules: List<Module> = _modules

    /**
     * Danh sách module thực sự cần xử lý packet (đang bật, hoặc đặt receivePacketsWhenDisabled).
     * Giữ đúng thứ tự đăng ký; được thay thế nguyên khối nên đọc từ thread khác là an toàn.
     */
    @Volatile
    private var _activeModules: Array<Module> = emptyArray()

    val activeModules: Array<Module>
        get() = _activeModules

    @Synchronized
    fun refreshActiveModules() {
        _activeModules = _modules
            .filter { it.isEnabled || it.receivePacketsWhenDisabled }
            .toTypedArray()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        with(_modules) {
            add(FlyModule())
            add(ESPModule())
            add(ZoomModule())
            add(AirJumpModule())
            add(NoClipModule())
            add(NightVisionModule())
            add(HasteModule())
            add(SpeedModule())
            add(JetPackModule())
            add(LevitationModule())
            add(HighJumpModule())
            add(SlowFallingModule())
            add(PoseidonModule())
            add(AntiKnockbackModule())
            add(RegenerationModule())
            add(BhopModule())
            add(SprintModule())
            add(NoHurtCameraModule())
            add(AutoWalkModule())
            add(AntiAFKModule())
            add(DesyncModule())
            add(PositionLoggerModule())
            add(MotionFlyModule())
            add(FreeCameraModule())
            add(KillauraModule())
            add(NauseaModule())
            add(HealthBoostModule())
            add(JumpBoostModule())
            add(ResistanceModule())
            add(FireResistanceModule())
            add(SwiftnessModule())
            add(InstantHealthModule())
            add(StrengthModule())
            add(InstantDamageModule())
            add(InvisibilityModule())
            add(SaturationModule())
            add(AbsorptionModule())
            add(BlindnessModule())
            add(AntiCrystalModule())
            add(HungerModule())
            add(WeaknessModule())
            add(PoisonModule())
            add(WitherModule())
            add(FatalPoisonModule())
            add(ConduitPowerModule())
            add(BadOmenModule())
            add(VillageHeroModule())
            add(DarknessModule())
            add(TimeShiftModule())
            add(WeatherControllerModule())
            add(FakeDeathModule())
            add(ExplosionParticleModule())
            add(BubbleParticleModule())
            add(HeartParticleModule())
            add(FakeXPModule())
            add(DustParticleModule())
            add(EyeOfEnderDeathParticleModule())
            add(FizzParticleModule())
            add(BreezeWindExplosionParticleModule())
            add(HitAndRunModule())
            add(HitboxModule())
            add(CrystalSmashModule())
            add(TriggerBotModule())
            add(NoChatModule())
            add(SpeedDisplayModule())
            add(PositionDisplayModule())
            add(CommandHandlerModule())
            add(NetworkInfoModule())
            add(MiningFatigueModule())
            add(WorldStateModule())
            add(ReplayModule())
            add(BaritoneModule())
            add(HudModule())
            add(WaypointModule())
            add(BreadcrumbsModule())
        }
        refreshActiveModules()
    }

    fun saveConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }

        writeAtomically(config, json.encodeToString(jsonObject))
    }

    /** Ghi ra file tạm rồi đổi tên để file không bị cụt nếu app bị tắt giữa chừng. */
    private fun writeAtomically(target: File, text: String) {
        val tempFile = File(target.parentFile, target.name + ".tmp")
        tempFile.writeText(text)
        if (!tempFile.renameTo(target)) {
            target.writeText(text)
            tempFile.delete()
        }
    }

    fun loadConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        if (!config.exists()) {
            return
        }

        // File cấu hình hỏng/thiếu "modules" thì giữ cấu hình mặc định, không làm crash app.
        try {
            val jsonString = config.readText()
            if (jsonString.isEmpty()) {
                return
            }

            val modules = json.parseToJsonElement(jsonString).jsonObject["modules"] as? JsonObject
                ?: return
            _modules.forEach { module ->
                (modules[module.name] as? JsonObject)?.let {
                    module.fromJson(it)
                }
            }
        } catch (e: Exception) {
            Log.e("ModuleManager", "Failed to load config, using defaults", e)
        }
    }

    fun exportConfig(): String {
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }
        return json.encodeToString(jsonObject)
    }

    fun importConfig(configStr: String) {
        try {
            val jsonObject = json.parseToJsonElement(configStr).jsonObject
            val modules = jsonObject["modules"]?.jsonObject ?: return

            _modules.forEach { module ->
                modules[module.name]?.let {
                    if (it is JsonObject) {
                        module.fromJson(it)
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid config format")
        }
    }


    // ---- Profile cấu hình (nhiều bộ cấu hình đặt tên, dùng qua lệnh .config) ----

    private val profileNameRegex = Regex("^[A-Za-z0-9_-]{1,32}$")

    fun isValidProfileName(name: String) = profileNameRegex.matches(name)

    private fun profilesDir(): File {
        return AppContext.instance.filesDir.resolve("profiles").also { it.mkdirs() }
    }

    private fun profileFile(name: String): File {
        require(isValidProfileName(name)) { "Invalid profile name" }
        return profilesDir().resolve("$name.json")
    }

    fun listProfiles(): List<String> {
        return profilesDir()
            .listFiles { file -> file.isFile && file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun saveProfile(name: String) {
        writeAtomically(profileFile(name), exportConfig())
    }

    /** Trả về false nếu profile không tồn tại; ném IllegalArgumentException nếu file hỏng. */
    fun loadProfile(name: String): Boolean {
        val file = profileFile(name)
        if (!file.exists()) {
            return false
        }
        importConfig(file.readText())
        return true
    }

    fun deleteProfile(name: String): Boolean {
        val file = profileFile(name)
        return file.exists() && file.delete()
    }

    fun exportConfigToFile(context: Context, fileName: String): Boolean {
        return try {
            val configsDir = context.getExternalFilesDir("configs")
            configsDir?.mkdirs()

            val configFile = File(configsDir, "$fileName.json")
            configFile.writeText(exportConfig())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importConfigFromFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val configStr = input.bufferedReader().readText()
                importConfig(configStr)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}