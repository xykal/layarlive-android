package com.arena.layarlive

import android.content.Context
import android.graphics.Color

/** Normalized placement; all four values are fractions of the live screen. */
data class LayerBox(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float
)

data class SceneConfig(
    var textEnabled: Boolean = true,
    var text: String = "LIVE • MAKASIH UDAH MAMPIR!",
    var textSizeSp: Float = 17f,
    var textColor: Int = Color.WHITE,
    var textBackground: Int = 0xD9161D30.toInt(),
    var textBox: LayerBox = LayerBox(0.06f, 0.09f, 0.84f, 0.095f),
    var saweriaEnabled: Boolean = false,
    var saweriaUrl: String = "",
    var saweriaBox: LayerBox = LayerBox(0.05f, 0.70f, 0.90f, 0.22f)
)

object SceneStore {
    private const val PREFS = "layarlive_scene_v2"

    fun load(context: Context): SceneConfig {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return SceneConfig(
            textEnabled = p.getBoolean("text_enabled", true),
            text = p.getString("text_value", "LIVE • MAKASIH UDAH MAMPIR!") ?: "LIVE • MAKASIH UDAH MAMPIR!",
            textSizeSp = p.getFloat("text_size", 17f),
            textColor = p.getInt("text_color", Color.WHITE),
            textBackground = p.getInt("text_background", 0xD9161D30.toInt()),
            textBox = LayerBox(
                p.getFloat("text_x", 0.06f),
                p.getFloat("text_y", 0.09f),
                p.getFloat("text_w", 0.84f),
                p.getFloat("text_h", 0.095f)
            ),
            saweriaEnabled = p.getBoolean("saweria_enabled", false),
            saweriaUrl = SecretStore.decrypt(p.getString("saweria_url_encrypted", "") ?: ""),
            saweriaBox = LayerBox(
                p.getFloat("widget_x", 0.05f),
                p.getFloat("widget_y", 0.70f),
                p.getFloat("widget_w", 0.90f),
                p.getFloat("widget_h", 0.22f)
            )
        )
    }

    fun save(context: Context, scene: SceneConfig) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putBoolean("text_enabled", scene.textEnabled)
            .putString("text_value", scene.text)
            .putFloat("text_size", scene.textSizeSp)
            .putInt("text_color", scene.textColor)
            .putInt("text_background", scene.textBackground)
            .putFloat("text_x", scene.textBox.x)
            .putFloat("text_y", scene.textBox.y)
            .putFloat("text_w", scene.textBox.width)
            .putFloat("text_h", scene.textBox.height)
            .putBoolean("saweria_enabled", scene.saweriaEnabled)
            .putString("saweria_url_encrypted", SecretStore.encrypt(scene.saweriaUrl))
            .putFloat("widget_x", scene.saweriaBox.x)
            .putFloat("widget_y", scene.saweriaBox.y)
            .putFloat("widget_w", scene.saweriaBox.width)
            .putFloat("widget_h", scene.saweriaBox.height)
            .apply()
    }
}
