package br.com.bloqueiototal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.PhoneAccount

class BlockAllCallsService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val preferences = BlockingPreferences(this)
        val canReadContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val contactStatus = when {
            preferences.mode != BlockingMode.UNSAVED_ONLY -> ContactStatus.UNKNOWN
            !canReadContacts -> ContactStatus.NOT_SAVED
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !callDetails.contactDisplayName.isNullOrBlank() -> ContactStatus.SAVED
            else -> ContactLookup.classify(this, callDetails.handle)
        }
        val response = ScreeningResponse.create(
            preferences.enabled,
            preferences.mode,
            callDetails.callDirection,
            callDetails.handle?.scheme,
            contactStatus
        ) ?: return // Outgoing and unknown-direction calls are never rejected.

        // No network, logging, UI or asynchronous work on the 5s path.
        respondToCall(callDetails, response)
    }
}

internal enum class ContactStatus { SAVED, NOT_SAVED, UNKNOWN }

internal object ScreeningResponse {
    fun create(
        enabled: Boolean,
        mode: BlockingMode,
        direction: Int,
        scheme: String?,
        contactStatus: ContactStatus
    ): CallScreeningService.CallResponse? {
        if (direction != Call.Details.DIRECTION_INCOMING) return null
        val includedByMode = mode == BlockingMode.ALL_CALLS || contactStatus == ContactStatus.NOT_SAVED
        val block = enabled && scheme == PhoneAccount.SCHEME_TEL && includedByMode
        return CallScreeningService.CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSkipNotification(block)
            // Keep the system's blocked-call history for diagnosis.
            .setSkipCallLog(false)
            .build()
    }
}

internal object ContactLookup {
    private val projection = arrayOf(ContactsContract.PhoneLookup._ID)

    fun classify(context: Context, handle: Uri?): ContactStatus {
        if (handle?.scheme != PhoneAccount.SCHEME_TEL) return ContactStatus.UNKNOWN
        val number = handle.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: return ContactStatus.UNKNOWN
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return try {
            val found = context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
            } ?: return ContactStatus.UNKNOWN
            if (found) ContactStatus.SAVED else ContactStatus.NOT_SAVED
        } catch (_: RuntimeException) {
            // In UNSAVED_ONLY mode, an unavailable contacts provider must fail open.
            ContactStatus.UNKNOWN
        }
    }
}
