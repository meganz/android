package test.mega.privacy.android.app.lollipop.managerSections.settings

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import mega.privacy.android.app.R
import mega.privacy.android.app.TestActivityModule
import mega.privacy.android.app.di.SettingsModule
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsActivity
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsFragment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withNoRowContaining
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withRowContaining
import test.mega.privacy.android.app.launchFragmentInHiltContainer


@HiltAndroidTest
@UninstallModules(SettingsModule::class, TestActivityModule::class)
class SettingsFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Module
    @InstallIn(ActivityRetainedComponent::class)
    object TestSettingsModule {
        @Provides
        fun provideGetAccountDetails(): GetAccountDetails = mock<GetAccountDetails>()

        val canDeleteAccount = mock<CanDeleteAccount>()

        @Provides
        fun provideCanDeleteAccount(): CanDeleteAccount = canDeleteAccount

        @Provides
        fun provideRefreshUserAccount(): RefreshUserAccount = mock<RefreshUserAccount>()

        @Provides
        fun provideRefreshPasscodeLockPreference(): RefreshPasscodeLockPreference =
            mock<RefreshPasscodeLockPreference>()

        @Provides
        fun provideIsLoggingEnabled(): IsLoggingEnabled = mock<IsLoggingEnabled>()

        @Provides
        fun provideIsChatLoggingEnabled(): IsChatLoggingEnabled = mock<IsChatLoggingEnabled>()

        @Provides
        fun provideIsCameraSyncEnabled(): IsCameraSyncEnabled = mock<IsCameraSyncEnabled>()

        @Provides
        fun provideRootNodeExists(): RootNodeExists = mock<RootNodeExists>()

        @Provides
        fun provideIsMultiFactorAuthAvailable(): IsMultiFactorAuthAvailable =
            mock<IsMultiFactorAuthAvailable>()

        @Provides
        fun provideFetchContactLinksOption(): FetchContactLinksOption =
            mock<FetchContactLinksOption>()

        @Provides
        fun providePerformMultiFactorAuthCheck(): PerformMultiFactorAuthCheck =
            mock<PerformMultiFactorAuthCheck>()

        @Provides
        fun provideSettingsActivity(): SettingsActivity = mock<SettingsActivity>()
    }

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun test_delete_preference_is_removed_if_account_cannot_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount.invoke()).thenReturn(false)
        launchFragmentInHiltContainer<SettingsFragment>()

        onView(withId(androidx.preference.R.id.recycler_view))
            .check(withNoRowContaining(withText(R.string.settings_delete_account)))
    }

    @Test
    fun test_delete_preference_is_present_if_account_can_be_deleted() {
        whenever(TestSettingsModule.canDeleteAccount.invoke()).thenReturn(true)
        launchFragmentInHiltContainer<SettingsFragment>()

        onView(withId(androidx.preference.R.id.recycler_view))
            .check(withRowContaining(withText(R.string.settings_delete_account)))
    }


}
