package test.mega.privacy.android.app.presentation.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.*
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.Suppress
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.StartScreenPreferencesActivity
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.presentation.settings.SettingsFragment
import mega.privacy.android.app.utils.Constants
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withNoRowContaining
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withRowContaining
import test.mega.privacy.android.app.TEST_USER_ACCOUNT
import test.mega.privacy.android.app.di.TestInitialiseUseCases
import test.mega.privacy.android.app.di.TestSettingsModule
import test.mega.privacy.android.app.launchFragmentInHiltContainer


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private val idlingResource = CountingIdlingResource("IdleCounter")
    private val hide = MutableStateFlow(false)
    private val initialScreen = 0
    private val startScreen = MutableStateFlow(initialScreen)


    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(idlingResource)
        initialiseMockDefaults()
        hiltRule.inject()
        Intents.init()
    }

    private fun initialiseMockDefaults() {
        whenever(TestSettingsModule.shouldHideRecentActivity()).thenReturn(hide)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        Intents.release()
    }

    @Test
    fun test_delete_preference_is_removed_if_account_cannot_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(false)
        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(withNoRowContaining(withText(R.string.settings_delete_account)))
    }

    @Test
    fun test_delete_preference_is_present_if_account_can_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)

        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(withRowContaining(withText(R.string.settings_delete_account)))
    }


    @Test
    fun test_that_when_can_delete_changes_to_true_preference_is_added_again() {
        val refreshUserAccount = UserAccount(
            email = "refreshEmail",
            isBusinessAccount = false,
            isMasterBusinessAccount = false,
            accountTypeIdentifier = Constants.FREE
        )

        whenever(TestSettingsModule.getAccountDetails(false)).thenReturn(TEST_USER_ACCOUNT)
        whenever(TestSettingsModule.getAccountDetails(true)).thenReturn(refreshUserAccount)

        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(false)
        whenever(TestSettingsModule.canDeleteAccount(refreshUserAccount)).thenReturn(true)

        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(withNoRowContaining(withText(R.string.settings_delete_account)))

//        This test occasionally failed due to timing issues.
//        Adding this receiver with an idling resource ensures that the verification only happens
//        after the broadcast has been received.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                idlingResource.decrement()
            }
        }

        scenario?.onActivity {
            it.registerReceiver(
                receiver,
                IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            )
            val intent = Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            it.sendBroadcast(intent)
            idlingResource.increment()
        }

        onPreferences()
            .check(withRowContaining(withText(R.string.settings_delete_account)))

        scenario?.onActivity {
            it.unregisterReceiver(
                receiver
            )
        }
    }

    @Ignore("Pending issue regarding download location")
    @Test
    fun test_that_download_location_is_included() {
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(withRowContaining(withText(R.string.download_location)))
    }


    @Test
    fun test_that_activated_delete_has_100_percent_alpha() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.settings_delete_account),
                        withTextColorAlpha(1.0)
                    )
                )
            )
    }

    @Test
    fun test_that_deactivated_delete_has_50_percent_alpha() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(flowOf(false))
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.settings_delete_account),
                        withTextColorAlpha(0.5)
                    )
                )
            )
    }


    @Test
    fun test_that_updating_start_screen_preference_updates_the_description() {
        val newStartScreen = 1
        launchFragmentInHiltContainer<SettingsFragment>()
        val startScreenDescriptionStrings =
            getApplicationContext<HiltTestApplication>().resources.getStringArray(
                R.array.settings_start_screen
            )

        onPreferences()
            .check(withRowContaining(withText(startScreenDescriptionStrings[initialScreen])))

        startScreen.tryEmit(newStartScreen)

        onPreferences()
            .check(withRowContaining(withText(startScreenDescriptionStrings[newStartScreen])))
    }

    @Test
    fun test_that_correct_fields_are_disable_when_offline() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)
        whenever(TestSettingsModule.isMultiFactorAuthAvailable()).thenReturn(true)
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(flowOf(false))
        launchFragmentInHiltContainer<SettingsFragment>()



        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.section_photo_sync), not(isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.section_chat), not(isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.settings_2fa), not(isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.section_qr_code), not(isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        withText(R.string.settings_delete_account), not(
                            isEnabled()
                        )
                    )
                )
            )

    }

    @Test
    fun test_that_hide_recent_activity_event_updates_hide_recent_activity() {
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        hasDescendant(withText(R.string.hide_recent_setting_context)),
                        hasSibling(hasDescendant(not(isChecked())))
                    )
                )
            )
        hide.tryEmit(true)
        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        hasDescendant(withText(R.string.hide_recent_setting_context)),
                        hasSibling(hasDescendant(isChecked()))
                    )
                )
            )
    }

    @Suppress
    @Test
    fun test_that_when_fragment_is_launched_and_openSettingsStorage_is_set_FileManagementPreferencesActivity_is_launched() {
        intending(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    FileManagementPreferencesActivity::class.java.name
                )
            )
        ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        val args = Bundle().apply {
            putString(
                SettingsFragment.INITIAL_PREFERENCE,
                SettingsConstants.KEY_STORAGE_FILE_MANAGEMENT
            )
            putBoolean(SettingsFragment.NAVIGATE_TO_INITIAL_PREFERENCE, true)
        }

        launchFragmentInHiltContainer<SettingsFragment>(args)

        intended(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    FileManagementPreferencesActivity::class.java.name
                )
            )
        )

    }

    @Test
    fun test_that_when_fragment_is_launched_and_openSettingsStartScreen_is_set_StartScreenPreferencesActivity_is_launched() {
        intending(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    StartScreenPreferencesActivity::class.java.name
                )
            )
        ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        val args = Bundle().apply {
            putString(
                SettingsFragment.INITIAL_PREFERENCE,
                SettingsConstants.KEY_START_SCREEN
            )
            putBoolean(SettingsFragment.NAVIGATE_TO_INITIAL_PREFERENCE, true)
        }

        launchFragmentInHiltContainer<SettingsFragment>(args)

        intended(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    StartScreenPreferencesActivity::class.java.name
                )
            )
        )
    }

    @Test
    fun test_that_2FA_updates_toggle_the_switch() {
        whenever(TestSettingsModule.isMultiFactorAuthAvailable()).thenReturn(true)
        whenever(TestSettingsModule.fetchMultiFactorAuthSetting()).thenReturn(flowOf(true))
        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(
                withRowContaining(
                    allOf(
                        hasDescendant(withText(R.string.setting_subtitle_2fa)),
                        hasSibling(hasDescendant(isChecked()))
                    )
                )
            )

    }
}

