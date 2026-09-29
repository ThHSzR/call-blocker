package br.com.bloqueiototal

import android.content.Context
import android.telecom.Call
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35])
class ScreeningTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before fun clearPreferences() {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences("blocking", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun allCallsModeRejectsSavedAndUnsavedIncomingCalls() {
        for (contactStatus in listOf(ContactStatus.SAVED, ContactStatus.NOT_SAVED, ContactStatus.UNKNOWN)) {
            val response = response(BlockingMode.ALL_CALLS, contactStatus)
            assertTrue(response.disallowCall)
            assertTrue(response.rejectCall)
            assertTrue(response.skipNotification)
            assertFalse(response.skipCallLog)
        }
    }

    @Test fun unsavedOnlyModeRejectsOnlyConfirmedUnsavedNumbers() {
        assertTrue(response(BlockingMode.UNSAVED_ONLY, ContactStatus.NOT_SAVED).disallowCall)
        assertFalse(response(BlockingMode.UNSAVED_ONLY, ContactStatus.SAVED).disallowCall)
        assertFalse(response(BlockingMode.UNSAVED_ONLY, ContactStatus.UNKNOWN).disallowCall)
    }

    @Test fun disabledSwitchAllowsIncomingCall() {
        val response = ScreeningResponse.create(
            false,
            BlockingMode.ALL_CALLS,
            Call.Details.DIRECTION_INCOMING,
            "tel",
            ContactStatus.NOT_SAVED
        )!!
        assertFalse(response.disallowCall)
        assertFalse(response.rejectCall)
        assertFalse(response.skipNotification)
    }

    @Test fun outgoingAndUnknownDirectionNeverGetARejection() {
        for (direction in listOf(Call.Details.DIRECTION_OUTGOING, Call.Details.DIRECTION_UNKNOWN)) {
            assertNull(ScreeningResponse.create(
                true, BlockingMode.ALL_CALLS, direction, "tel", ContactStatus.NOT_SAVED
            ))
        }
    }

    @Test fun otherSchemesAndMissingHandleAreAllowed() {
        for (scheme in listOf("sip", "https", null)) {
            val response = ScreeningResponse.create(
                true,
                BlockingMode.ALL_CALLS,
                Call.Details.DIRECTION_INCOMING,
                scheme,
                ContactStatus.NOT_SAVED
            )!!
            assertFalse(response.disallowCall)
            assertFalse(response.rejectCall)
            assertFalse(response.skipNotification)
        }
    }

    @Test fun switchAndModePersistAcrossPreferenceInstances() {
        val preferences = BlockingPreferences(context)
        assertFalse(preferences.enabled)
        assertEquals(BlockingMode.ALL_CALLS, preferences.mode)

        preferences.enabled = true
        preferences.mode = BlockingMode.UNSAVED_ONLY
        assertTrue(BlockingPreferences(context).enabled)
        assertEquals(BlockingMode.UNSAVED_ONLY, BlockingPreferences(context).mode)

        preferences.enabled = false
        preferences.mode = BlockingMode.ALL_CALLS
        assertFalse(BlockingPreferences(context).enabled)
        assertEquals(BlockingMode.ALL_CALLS, BlockingPreferences(context).mode)
    }

    private fun response(mode: BlockingMode, contactStatus: ContactStatus) =
        ScreeningResponse.create(
            true,
            mode,
            Call.Details.DIRECTION_INCOMING,
            "tel",
            contactStatus
        )!!
}
