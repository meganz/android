package test.mega.privacy.android.app.presentation.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.Suppress
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.StartScreenPreferencesActivity
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.presentation.settings.SettingsFragment
import mega.privacy.android.app.presentation.settings.reportissue.ReportIssueFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.RecyclerViewAssertions
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
    private val mediaDiscoveryViewState = MutableStateFlow(0)
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
        whenever(TestSettingsModule.monitorHideRecentActivity()).thenReturn(hide)
        whenever(TestSettingsModule.monitorMediaDiscoveryView()).thenReturn(mediaDiscoveryViewState)
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
            .check(RecyclerViewAssertions.withNoRowContaining(ViewMatchers.withText(R.string.settings_delete_account)))
    }

    @Test
    fun test_delete_preference_is_present_if_account_can_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)

        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(R.string.settings_delete_account)))
    }


    @Test
    fun test_that_when_can_delete_changes_to_true_preference_is_added_again() {
        val refreshUserAccount = UserAccount(
            userId = UserId(2),
            email = "refreshEmail",
            fullName = "name",
            isBusinessAccount = false,
            isMasterBusinessAccount = false,
            accountTypeIdentifier = AccountType.FREE,
            accountTypeString = "free",
        )

        runBlocking {
            whenever(TestSettingsModule.getAccountDetails(false)).thenReturn(TEST_USER_ACCOUNT)
            whenever(TestSettingsModule.getAccountDetails(true)).thenReturn(refreshUserAccount)
        }

        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(false)
        whenever(TestSettingsModule.canDeleteAccount(refreshUserAccount)).thenReturn(true)

        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(RecyclerViewAssertions.withNoRowContaining(ViewMatchers.withText(R.string.settings_delete_account)))

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
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(R.string.settings_delete_account)))

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
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(R.string.download_location)))
    }


    @Test
    fun test_that_activated_delete_has_100_percent_alpha() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(MutableStateFlow(true))
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.settings_delete_account),
                        withTextColorAlpha(1.0)
                    )
                )
            )
    }

    @Test
    fun test_that_deactivated_delete_has_50_percent_alpha() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(MutableStateFlow(false))
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.settings_delete_account),
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
            ApplicationProvider.getApplicationContext<HiltTestApplication>().resources.getStringArray(
                R.array.settings_start_screen
            )

        onPreferences()
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(
                startScreenDescriptionStrings[initialScreen])))

        startScreen.tryEmit(newStartScreen)

        onPreferences()
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(
                startScreenDescriptionStrings[newStartScreen])))
    }

    @Test
    fun test_that_correct_fields_are_disable_when_offline() {
        whenever(TestSettingsModule.canDeleteAccount(TEST_USER_ACCOUNT)).thenReturn(true)
        whenever(TestSettingsModule.isMultiFactorAuthAvailable()).thenReturn(true)
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(MutableStateFlow(false))
        launchFragmentInHiltContainer<SettingsFragment>()



        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.section_photo_sync),
                        Matchers.not(ViewMatchers.isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.section_chat),
                        Matchers.not(ViewMatchers.isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.settings_2fa),
                        Matchers.not(ViewMatchers.isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.section_qr_code),
                        Matchers.not(ViewMatchers.isEnabled())
                    )
                )
            )

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.settings_delete_account), Matchers.not(
                            ViewMatchers.isEnabled()
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
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.hide_recent_setting_context)),
                        ViewMatchers.hasSibling(ViewMatchers.hasDescendant(Matchers.not(ViewMatchers.isChecked())))
                    )
                )
            )
        hide.tryEmit(true)
        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.hide_recent_setting_context)),
                        ViewMatchers.hasSibling(ViewMatchers.hasDescendant(ViewMatchers.isChecked()))
                    )
                )
            )
    }

    @Suppress
    @Test
    fun test_that_when_fragment_is_launched_and_openSettingsStorage_is_set_FileManagementPreferencesActivity_is_launched() {
        Intents.intending(
            IntentMatchers.hasComponent(
                ComponentName(
                    ApplicationProvider.getApplicationContext<HiltTestApplication>(),
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

        Intents.intended(
            IntentMatchers.hasComponent(
                ComponentName(
                    ApplicationProvider.getApplicationContext<HiltTestApplication>(),
                    FileManagementPreferencesActivity::class.java.name
                )
            )
        )

    }

    @Test
    fun test_that_when_fragment_is_launched_and_openSettingsStartScreen_is_set_StartScreenPreferencesActivity_is_launched() {
        Intents.intending(
            IntentMatchers.hasComponent(
                ComponentName(
                    ApplicationProvider.getApplicationContext<HiltTestApplication>(),
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

        Intents.intended(
            IntentMatchers.hasComponent(
                ComponentName(
                    ApplicationProvider.getApplicationContext<HiltTestApplication>(),
                    StartScreenPreferencesActivity::class.java.name
                )
            )
        )
    }

    @Test
    fun test_that_2FA_updates_toggle_the_switch() {
        whenever(TestSettingsModule.isMultiFactorAuthAvailable()).thenReturn(true)
        runBlocking {
            whenever(TestSettingsModule.fetchMultiFactorAuthSetting()).thenReturn(true)
        }
        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    Matchers.allOf(
                        ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.setting_subtitle_2fa)),
                        ViewMatchers.hasSibling(ViewMatchers.hasDescendant(ViewMatchers.isChecked()))
                    )
                )
            )

    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun test_that_report_issue_is_visible_in_pre_30() {
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(R.string.settings_help_report_issue)))
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun test_that_report_issue_is_visible_in_30() {
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(RecyclerViewAssertions.withRowContaining(ViewMatchers.withText(R.string.settings_help_report_issue)))
    }

    @Test
    fun test_that_report_issue_result_displays_a_snackbar() {
        var resultHandler: FragmentResultListener? = null

        val scenario = launchFragmentInHiltContainer<SettingsFragment>() {
            resultHandler = this as FragmentResultListener
        }
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        val resultMessage = "This is a success"
        resultHandler?.onFragmentResult(SettingsConstants.REPORT_ISSUE,
            bundleOf(ReportIssueFragment::class.java.name to resultMessage))


        Espresso.onView(ViewMatchers.withText(resultMessage))
            .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

}