package br.com.bloqueiototal

import android.content.Context

enum class BlockingMode {
    UNSAVED_ONLY,
    ALL_CALLS
}

class BlockingPreferences(context: Context) {
    // Settings are available before the first unlock after reboot.
    // No phone numbers or contacts are stored.
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("blocking", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    var mode: BlockingMode
        get() = prefs.getString("mode", null)
            ?.let { stored -> BlockingMode.entries.firstOrNull { it.name == stored } }
            ?: BlockingMode.ALL_CALLS
        set(value) { prefs.edit().putString("mode", value.name).apply() }
}
