package br.com.bloqueiototal

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.PhoneAccount

class BlockAllCallsService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val response = ScreeningResponse.create(
            BlockingPreferences(this).enabled,
            callDetails.callDirection,
            callDetails.handle?.scheme
        ) ?: return // Outgoing and unknown-direction calls are never rejected.

        // No network, contact query, logging, UI or asynchronous work on the 5s path.
        respondToCall(callDetails, response)
    }
}

internal object ScreeningResponse {
    fun create(enabled: Boolean, direction: Int, scheme: String?): CallScreeningService.CallResponse? {
        if (direction != Call.Details.DIRECTION_INCOMING) return null
        val block = enabled && scheme == PhoneAccount.SCHEME_TEL
        return CallScreeningService.CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSkipNotification(block)
            // Keep the system's blocked-call history for diagnosis.
            .setSkipCallLog(false)
            .build()
    }
}
