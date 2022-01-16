package test.mega.privacy.android.app.lollipop.managerSections.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.*
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.Suppress
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.TestActivityModule
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.StartScreenPreferencesActivity
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.di.SettingsModule
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsActivity
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsFragment
import mega.privacy.android.app.utils.Constants
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withNoRowContaining
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withRowContaining
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import test.mega.privacy.android.app.testFragment
import test.mega.privacy.android.app.testFragmentTag
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(SettingsModule::class, TestActivityModule::class)
@kotlin.Suppress("DEPRECATION")
class SettingsFragmentTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private val idlingResource = CountingIdlingResource("IdleCounter")

    private val userAccount = UserAccount(
        email = "email",
        isBusinessAccount = false,
        isMasterBusinessAccount = false,
        accountTypeIdentifier = Constants.FREE
    )

    @Module
    @InstallIn(SingletonComponent::class)
    object TestSettingsModule {
        val canDeleteAccount = mock<CanDeleteAccount>()
        val getStartScreen = mock<GetStartScreen>()
        val isMultiFactorAuthAvailable = mock<IsMultiFactorAuthAvailable>()
        val settingsActivity = mock<SettingsActivity>()
        val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
        val fetchMultiFactorAuthSetting = mock<FetchMultiFactorAuthSetting>()
        val getAccountDetails = mock<GetAccountDetails>()
        val isOnline = mock<IsOnline>()
        val rootNodeExists = mock<RootNodeExists>()

        @Provides
        fun provideSettingsActivity(): SettingsActivity = settingsActivity


        @Provides
        fun provideGetAccountDetails(): GetAccountDetails = getAccountDetails


        @Provides
        fun provideCanDeleteAccount(): CanDeleteAccount = canDeleteAccount

        @Provides
        fun provideRefreshPasscodeLockPreference(): RefreshPasscodeLockPreference =
            mock()

        @Provides
        fun provideIsLoggingEnabled(): IsLoggingEnabled = mock()

        @Provides
        fun provideIsChatLoggingEnabled(): IsChatLoggingEnabled = mock()

        @Provides
        fun provideIsCameraSyncEnabled(): IsCameraSyncEnabled = mock()


        @Provides
        fun provideRootNodeExists(): RootNodeExists = rootNodeExists


        @Provides
        fun provideIsMultiFactorAuthAvailable(): IsMultiFactorAuthAvailable =
            isMultiFactorAuthAvailable


        @Provides
        fun provideFetchContactLinksOption(): FetchAutoAcceptQRLinks =
            fetchAutoAcceptQRLinks


        @Provides
        fun provideFetchMultiFactorAuthSetting(): FetchMultiFactorAuthSetting =
            fetchMultiFactorAuthSetting


        @Provides
        fun provideGetStartScreen(): GetStartScreen = getStartScreen

        @Provides
        fun provideShouldHideRecentActivity(): ShouldHideRecentActivity =
            mock()

        @Provides
        fun provideToggleAutoAcceptQRLinks(): ToggleAutoAcceptQRLinks =
            mock()


        @Provides
        fun provideIsOnline(): IsOnline = isOnline

    }

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(idlingResource)
        initialiseMockDefaults()
        hiltRule.inject()
        Intents.init()
    }

    private fun initialiseMockDefaults() {
        runBlocking { whenever(TestSettingsModule.fetchAutoAcceptQRLinks()).thenReturn(false) }

        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(true)
        whenever(TestSettingsModule.isMultiFactorAuthAvailable()).thenReturn(true)
        whenever(TestSettingsModule.isOnline()).thenReturn(flowOf(true))
        whenever(TestSettingsModule.fetchMultiFactorAuthSetting()).thenReturn(flowOf())
        whenever(TestSettingsModule.getAccountDetails(any())).thenReturn(userAccount)
        whenever(TestSettingsModule.rootNodeExists()).thenReturn(true)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        Intents.release()
        Mockito.reset(
            TestSettingsModule.getStartScreen,
            TestSettingsModule.isMultiFactorAuthAvailable,
            TestSettingsModule.fetchMultiFactorAuthSetting,
            TestSettingsModule.settingsActivity,
            TestSettingsModule.canDeleteAccount,
            TestSettingsModule.getAccountDetails,
            TestSettingsModule.isOnline,
            TestSettingsModule.rootNodeExists,
        )
    }

    @Test
    fun test_delete_preference_is_removed_if_account_cannot_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(false)
        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(withNoRowContaining(withText(R.string.settings_delete_account)))
    }

    @Test
    fun test_delete_preference_is_present_if_account_can_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(true)

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

        whenever(TestSettingsModule.getAccountDetails(false)).thenReturn(userAccount)
        whenever(TestSettingsModule.getAccountDetails(true)).thenReturn(refreshUserAccount)

        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(false)
        whenever(TestSettingsModule.canDeleteAccount(refreshUserAccount)).thenReturn(true)

        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        onPreferences()
            .check(withNoRowContaining(withText(R.string.settings_delete_account)))

//        This test occasionally fails due to timing issues.
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

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun test_that_download_location_is_included_pre_30() {
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(withRowContaining(withText(R.string.download_location)))
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun test_that_download_location_is_excluded_post_30() {
        launchFragmentInHiltContainer<SettingsFragment>()

        onPreferences()
            .check(withNoRowContaining(withText(R.string.download_location)))
    }

    @Test
    fun test_that_activated_delete_has_100_percent_alpha() {
        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(true)
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
        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(true)
        whenever(TestSettingsModule.isOnline()).thenReturn(flowOf(false))
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
        val initialScreen = 0
        val newStartScreen = 1
        whenever(TestSettingsModule.getStartScreen()).thenReturn(initialScreen)
        val scenario = launchFragmentInHiltContainer<SettingsFragment>()
        val startScreenDescriptionStrings =
            getApplicationContext<HiltTestApplication>().resources.getStringArray(
                R.array.settings_start_screen
            )

        onPreferences()
            .check(withRowContaining(withText(startScreenDescriptionStrings[initialScreen])))

        scenario?.onActivity {
            val fragment = it.testFragment<SettingsFragment>()
            fragment.updateStartScreenSetting(newStartScreen)
        }

        onPreferences()
            .check(withRowContaining(withText(startScreenDescriptionStrings[newStartScreen])))
    }

    @Test
    fun test_that_correct_fields_are_disable_when_offline() {
        whenever(TestSettingsModule.canDeleteAccount(userAccount)).thenReturn(true)
        whenever(TestSettingsModule.isMultiFactorAuthAvailable()).thenReturn(true)
        whenever(TestSettingsModule.isOnline()).thenReturn(flowOf(false))
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
    fun test_that_start_screen_update_event_updates_start_screen() {
        val initialScreen = 0
        val newStartScreen = 1
        whenever(TestSettingsModule.getStartScreen()).thenReturn(initialScreen)
        launchFragmentInHiltContainer<SettingsFragment>()
        val startScreenDescriptionStrings =
            getApplicationContext<HiltTestApplication>().resources.getStringArray(
                R.array.settings_start_screen
            )

        onPreferences()
            .check(withRowContaining(withText(startScreenDescriptionStrings[initialScreen])))

        LiveEventBus.get(EventConstants.EVENT_UPDATE_START_SCREEN, Int::class.java)
            .post(newStartScreen)

        onPreferences()
            .check(withRowContaining(withText(startScreenDescriptionStrings[newStartScreen])))
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

        LiveEventBus.get(EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
            .post(true)

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
    fun test_that_when_fragment_is_launched_and_openSettingsStorage_is_true_FileManagementPreferencesActivity_is_launched() {
        whenever(TestSettingsModule.settingsActivity.openSettingsStorage).thenReturn(true)
        intending(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    FileManagementPreferencesActivity::class.java.name
                )
            )
        ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        launchFragmentInHiltContainer<SettingsFragment>()

        intended(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    FileManagementPreferencesActivity::class.java.name
                )
            )
        )

        verify(TestSettingsModule.settingsActivity, times(1)).openSettingsStartScreen = false
    }

    @Test
    fun test_that_when_fragment_is_launched_and_openSettingsStartScreen_is_true_StartScreenPreferencesActivity_is_launched() {
        whenever(TestSettingsModule.settingsActivity.openSettingsStartScreen).thenReturn(true)
        intending(
            hasComponent(
                ComponentName(
                    getApplicationContext<HiltTestApplication>(),
                    StartScreenPreferencesActivity::class.java.name
                )
            )
        ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        launchFragmentInHiltContainer<SettingsFragment>()

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


    private fun onPreferences() = onView(withId(androidx.preference.R.id.recycler_view))
}

