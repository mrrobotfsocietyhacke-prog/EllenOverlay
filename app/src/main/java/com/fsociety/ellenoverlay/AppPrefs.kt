package com.fsociety.ellenoverlay

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ellen_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var baseUrl: String
        get() = prefs.getString("base_url", "https://api.openai.com/v1") ?: ""
        set(value) = prefs.edit().putString("base_url", value.trimEnd('/')).apply()

    var model: String
        get() = prefs.getString("model", "gpt-4o-mini") ?: ""
        set(value) = prefs.edit().putString("model", value).apply()

    var systemPrompt: String
        get() = prefs.getString(
            "system_prompt",
            "Ты Эллен Джо из Zenless Zone Zero. Отвечай по-русски, естественно, коротко, с характером и без лишних объяснений."
        ) ?: ""
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    var overlayX: Int
        get() = prefs.getInt("overlay_x", 32)
        set(value) = prefs.edit().putInt("overlay_x", value).apply()

    var overlayY: Int
        get() = prefs.getInt("overlay_y", 160)
        set(value) = prefs.edit().putInt("overlay_y", value).apply()

    var overlayScale: Float
        get() = prefs.getFloat("overlay_scale", 1.0f)
        set(value) = prefs.edit().putFloat("overlay_scale", value.coerceIn(0.3f, 1.5f)).apply()
}
