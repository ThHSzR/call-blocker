package br.com.bloqueiototal

import android.content.Context
import android.telecom.Call
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35])
class ScreeningTest {
    @Test fun incomingTelephoneCallIsRejectedWithoutMissedCallNotification() {
        val r = ScreeningResponse.create(true, Call.Details.DIRECTION_INCOMING, "tel")!!
        assertTrue(r.disallowCall)
        assertTrue(r.rejectCall)
        assertTrue(r.skipNotification)
        assertFalse(r.skipCallLog)
    }

    @Test fun disabledSwitchAllowsIncomingCall() {
        val r = ScreeningResponse.create(false, Call.Details.DIRECTION_INCOMING, "tel")!!
        assertFalse(r.disallowCall)
        assertFalse(r.rejectCall)
        assertFalse(r.skipNotification)
    }

    @Test fun outgoingAndUnknownDirectionNeverGetARejection() {
        for (enabled in listOf(true, false)) {
            assertNull(ScreeningResponse.create(enabled, Call.Details.DIRECTION_OUTGOING, "tel"))
            assertNull(ScreeningResponse.create(enabled, Call.Details.DIRECTION_UNKNOWN, "tel"))
        }
    }

    @Test fun otherSchemesAndMissingHandleAreAllowed() {
        for (scheme in listOf("sip", "https", null)) {
            val r = ScreeningResponse.create(true, Call.Details.DIRECTION_INCOMING, scheme)!!
            assertFalse(r.disallowCall)
            assertFalse(r.rejectCall)
            assertFalse(r.skipNotification)
        }
    }

    @Test fun switchDefaultsOffAndPersistsAcrossPreferenceInstances() {
        val context: Context = RuntimeEnvironment.getApplication()
        context.createDeviceProtectedStorageContext().getSharedPreferences("blocking", Context.MODE_PRIVATE)
            .edit().clear().commit()
        assertFalse(BlockingPreferences(context).enabled)
        BlockingPreferences(context).enabled = true
        assertTrue(BlockingPreferences(context).enabled)
        BlockingPreferences(context).enabled = false
        assertFalse(BlockingPreferences(context).enabled)
    }
}
