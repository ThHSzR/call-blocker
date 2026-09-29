package br.com.bloqueiototal

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActivationTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val roleManager get() = app.getSystemService(RoleManager::class.java)

    @Before fun clearPreferences() {
        app.createDeviceProtectedStorageContext()
            .getSharedPreferences("blocking", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun switchActivatesOnlyWithRoleAndContactsAndSurvivesReopening() {
        shadowOf(roleManager).addHeldRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        BlockingPreferences(app).mode = BlockingMode.ALL_CALLS
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().findViewById<Switch>(R.id.block_switch).performClick()
        assertTrue(BlockingPreferences(app).enabled)
        controller.pause().stop().destroy()
        val reopened = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertTrue(reopened.get().findViewById<Switch>(R.id.block_switch).isChecked)
        reopened.get().findViewById<Switch>(R.id.block_switch).performClick()
        assertFalse(BlockingPreferences(app).enabled)
        reopened.pause().stop().destroy()
    }

    @Test fun missingContactsDoesNotSilentlyActivatePartialBlocking() {
        shadowOf(roleManager).addHeldRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS)
        BlockingPreferences(app).mode = BlockingMode.ALL_CALLS
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().findViewById<Switch>(R.id.block_switch).performClick()
        assertFalse(BlockingPreferences(app).enabled)
        assertFalse(controller.get().findViewById<Switch>(R.id.block_switch).isChecked)
        controller.pause().stop().destroy()
    }

    @Test fun unsavedOnlyModeActivatesWithoutContactsPermission() {
        shadowOf(roleManager).addHeldRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS)
        BlockingPreferences(app).mode = BlockingMode.UNSAVED_ONLY
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        activity.findViewById<Switch>(R.id.block_switch).performClick()

        assertTrue(BlockingPreferences(app).enabled)
        assertEquals(activity.getString(R.string.status_on_unsaved),
            activity.findViewById<TextView>(R.id.status).text.toString())
        assertEquals(View.GONE, activity.findViewById<Button>(R.id.contacts_button).visibility)
        controller.pause().stop().destroy()
    }

    @Test fun selectedModePersistsWhenActivityIsReopened() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().findViewById<RadioButton>(R.id.mode_unsaved).performClick()
        assertEquals(BlockingMode.UNSAVED_ONLY, BlockingPreferences(app).mode)
        controller.pause().stop().destroy()

        val reopened = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertTrue(reopened.get().findViewById<RadioButton>(R.id.mode_unsaved).isChecked)
        reopened.pause().stop().destroy()
    }

    @Test fun revokingPrerequisitesUpdatesEffectiveStatusOnResume() {
        shadowOf(roleManager).addHeldRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        BlockingPreferences(app).apply {
            enabled = true
            mode = BlockingMode.ALL_CALLS
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        fun status() = activity.findViewById<TextView>(R.id.status).text.toString()
        assertEquals(activity.getString(R.string.status_on_all), status())
        controller.pause()
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS)
        controller.resume()
        assertEquals(activity.getString(R.string.status_partial), status())
        controller.pause()
        shadowOf(roleManager).removeHeldRole(RoleManager.ROLE_CALL_SCREENING)
        controller.resume()
        assertEquals(activity.getString(R.string.status_no_role), status())
        controller.pause().stop().destroy()
    }
}
