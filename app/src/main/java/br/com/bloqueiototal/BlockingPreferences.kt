package br.com.bloqueiototal

import android.content.Context

class BlockingPreferences(context: Context) {
    // The one boolean is available before the first unlock after reboot.
    // No phone numbers or contacts are stored.
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("blocking", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
}
