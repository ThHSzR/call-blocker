package br.com.bloqueiototal

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

@Suppress("DEPRECATION") // Framework-only UI and result callbacks keep the APK minimal.
class MainActivity : Activity() {
    private lateinit var prefs: BlockingPreferences
    private lateinit var toggle: Switch
    private lateinit var status: TextView
    private var updating = false
    private var pendingActivation = false
    private val roles get() = getSystemService(RoleManager::class.java)
    private fun hasRole() = roles?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    private fun hasContacts() = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = BlockingPreferences(this)
        pendingActivation = savedInstanceState?.getBoolean("pending") ?: false
        setContentView(R.layout.activity_main)
        // Respect status/navigation bars on Android 15/16 with edge-to-edge enforcement.
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        toggle = findViewById(R.id.block_switch)
        status = findViewById(R.id.status)
        toggle.setOnCheckedChangeListener { _, checked ->
            if (!updating) {
                if (checked) {
                    pendingActivation = true
                    continueActivation()
                } else {
                    pendingActivation = false
                    prefs.enabled = false
                    refresh()
                }
            }
        }
        findViewById<Button>(R.id.role_button).setOnClickListener { requestScreeningRole() }
        findViewById<Button>(R.id.contacts_button).setOnClickListener { requestContacts() }
        findViewById<Button>(R.id.settings_button).setOnClickListener { openAppSettings() }
        refresh()
    }

    override fun onResume() { super.onResume(); if (::prefs.isInitialized) refresh() }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending", pendingActivation)
        super.onSaveInstanceState(outState)
    }

    private fun continueActivation() {
        when {
            !hasRole() -> requestScreeningRole()
            !hasContacts() -> requestContacts()
            else -> { prefs.enabled = true; pendingActivation = false }
        }
        refresh()
    }

    private fun requestScreeningRole() {
        val manager = roles
        if (manager == null || !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            cancelActivation(R.string.role_unavailable)
            return
        }
        if (hasRole()) { refresh(); return }
        try {
            startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), ROLE_REQUEST)
        } catch (_: ActivityNotFoundException) {
            cancelActivation(R.string.role_unavailable)
        }
    }

    private fun requestContacts() {
        if (hasContacts()) { refresh(); return }
        val asked = getPreferences(MODE_PRIVATE).getBoolean("asked_contacts", false)
        if (asked && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            pendingActivation = false
            AlertDialog.Builder(this)
                .setTitle(R.string.contacts_title)
                .setMessage(R.string.contacts_settings_help)
                .setPositiveButton(R.string.open_settings) { _, _ -> openAppSettings() }
                .setNegativeButton(android.R.string.cancel, null).show()
        } else {
            getPreferences(MODE_PRIVATE).edit().putBoolean("asked_contacts", true).apply()
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), CONTACTS_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ROLE_REQUEST) {
            if (hasRole() && pendingActivation) continueActivation()
            else if (!hasRole()) cancelActivation(R.string.role_denied)
            refresh()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_REQUEST) {
            if (hasContacts() && pendingActivation) continueActivation()
            else if (!hasContacts()) cancelActivation(R.string.contacts_denied)
            refresh()
        }
    }

    private fun cancelActivation(message: Int) {
        pendingActivation = false
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        refresh()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun refresh() {
        updating = true
        toggle.isChecked = prefs.enabled
        updating = false
        val role = hasRole()
        val contacts = hasContacts()
        status.setText(when {
            !role -> R.string.status_no_role
            prefs.enabled && !contacts -> R.string.status_partial
            prefs.enabled -> R.string.status_on
            else -> R.string.status_off
        })
        findViewById<TextView>(R.id.requirements).text = getString(
            R.string.requirements, getString(if (role) R.string.yes else R.string.no),
            getString(if (contacts) R.string.yes else R.string.no)
        )
        findViewById<Button>(R.id.role_button).isEnabled = !role
        findViewById<Button>(R.id.contacts_button).isEnabled = !contacts
    }

    private companion object {
        const val ROLE_REQUEST = 100
        const val CONTACTS_REQUEST = 101
    }
}
