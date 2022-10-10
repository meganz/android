package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.DefaultCanDeleteAccount
import mega.privacy.android.domain.usecase.DefaultFetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.DefaultGetAccountDetails
import mega.privacy.android.domain.usecase.DefaultGetStartScreen
import mega.privacy.android.domain.usecase.DefaultIsChatLoggedIn
import mega.privacy.android.domain.usecase.DefaultIsHideRecentActivityEnabled
import mega.privacy.android.domain.usecase.DefaultMonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.DefaultRefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.DefaultToggleAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.GetStartScreen
import mega.privacy.android.domain.usecase.IsCameraSyncEnabled
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsHideRecentActivityEnabled
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.PutPreference
import mega.privacy.android.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.SetCallsSoundNotifications
import mega.privacy.android.domain.usecase.SetChatImageQuality
import mega.privacy.android.domain.usecase.SetHideRecentActivity
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks

/**
 * Settings use cases module
 *
 * Provides use cases used by the [mega.privacy.android.app.presentation.settings.SettingsViewModel]
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsUseCases {

    @Binds
    abstract fun bindGetAccountDetails(useCase: DefaultGetAccountDetails): GetAccountDetails

    @Binds
    abstract fun bindCanDeleteAccount(useCase: DefaultCanDeleteAccount): CanDeleteAccount

    @Binds
    abstract fun bindRefreshPasscodeLockPreference(useCase: DefaultRefreshPasscodeLockPreference): RefreshPasscodeLockPreference

    @Binds
    abstract fun bindStartScreen(useCase: DefaultGetStartScreen): GetStartScreen

    @Binds
    abstract fun bindIsHideRecentActivityEnabled(useCase: DefaultIsHideRecentActivityEnabled): IsHideRecentActivityEnabled

    @Binds
    abstract fun bindToggleAutoAcceptQRLinks(useCase: DefaultToggleAutoAcceptQRLinks): ToggleAutoAcceptQRLinks

    @Binds
    abstract fun bindIsChatLoggedIn(useCase: DefaultIsChatLoggedIn): IsChatLoggedIn

    @Binds
    abstract fun bindFetchMultiFactorAuthSetting(useCase: DefaultFetchMultiFactorAuthSetting): FetchMultiFactorAuthSetting

    @Binds
    abstract fun bindMonitorAutoAcceptQRLinks(implementation: DefaultMonitorAutoAcceptQRLinks): MonitorAutoAcceptQRLinks

    companion object {
        @Provides
        fun providePutStringPreference(settingsRepository: SettingsRepository): PutPreference<String> =
            PutPreference(settingsRepository::setStringPreference)

        @Provides
        fun providePutStringSetPreference(settingsRepository: SettingsRepository): PutPreference<MutableSet<String>> =
            PutPreference(settingsRepository::setStringSetPreference)

        @Provides
        fun providePutIntPreference(settingsRepository: SettingsRepository): PutPreference<Int> =
            PutPreference(settingsRepository::setIntPreference)

        @Provides
        fun providePutLongPreference(settingsRepository: SettingsRepository): PutPreference<Long> =
            PutPreference(settingsRepository::setLongPreference)

        @Provides
        fun providePutFloatPreference(settingsRepository: SettingsRepository): PutPreference<Float> =
            PutPreference(settingsRepository::setFloatPreference)

        @Provides
        fun providePutBooleanPreference(settingsRepository: SettingsRepository): PutPreference<Boolean> =
            PutPreference(settingsRepository::setBooleanPreference)

        @Provides
        fun provideGetStringPreference(settingsRepository: SettingsRepository): GetPreference<String?> =
            GetPreference(settingsRepository::monitorStringPreference)

        @Provides
        fun provideGetStringSetPreference(settingsRepository: SettingsRepository): GetPreference<MutableSet<String>?> =
            GetPreference(settingsRepository::monitorStringSetPreference)

        @Provides
        fun provideGetIntPreference(settingsRepository: SettingsRepository): GetPreference<Int> =
            GetPreference(settingsRepository::monitorIntPreference)

        @Provides
        fun provideGetLongPreference(settingsRepository: SettingsRepository): GetPreference<Long> =
            GetPreference(settingsRepository::monitorLongPreference)

        @Provides
        fun provideGetFloatPreference(settingsRepository: SettingsRepository): GetPreference<Float> =
            GetPreference(settingsRepository::monitorFloatPreference)

        @Provides
        fun provideGetBooleanPreference(settingsRepository: SettingsRepository): GetPreference<Boolean> =
            GetPreference(settingsRepository::monitorBooleanPreference)

        @Provides
        fun provideFetchAutoAcceptQRLinks(settingsRepository: SettingsRepository): FetchAutoAcceptQRLinks =
            FetchAutoAcceptQRLinks(settingsRepository::fetchContactLinksOption)

        @Provides
        fun provideIsCameraSyncEnabled(settingsRepository: SettingsRepository): IsCameraSyncEnabled =
            IsCameraSyncEnabled(settingsRepository::isCameraSyncPreferenceEnabled)

        @Provides
        fun provideIsMultiFactorAuthAvailable(accountRepository: AccountRepository): IsMultiFactorAuthAvailable =
            IsMultiFactorAuthAvailable(accountRepository::isMultiFactorAuthAvailable)

        @Provides
        fun provideRequestAccountDeletion(accountRepository: AccountRepository): RequestAccountDeletion =
            RequestAccountDeletion(accountRepository::requestDeleteAccountLink)

        @Provides
        fun provideGetChatImageQuality(settingsRepository: SettingsRepository): GetChatImageQuality =
            GetChatImageQuality(settingsRepository::getChatImageQuality)

        @Provides
        fun provideSetChatImageQuality(settingsRepository: SettingsRepository): SetChatImageQuality =
            SetChatImageQuality(settingsRepository::setChatImageQuality)

        @Provides
        fun provideGetCallsSoundNotifications(settingsRepository: SettingsRepository): GetCallsSoundNotifications =
            GetCallsSoundNotifications(settingsRepository::getCallsSoundNotifications)

        @Provides
        fun provideSetCallsSoundNotifications(settingsRepository: SettingsRepository): SetCallsSoundNotifications =
            SetCallsSoundNotifications(settingsRepository::setCallsSoundNotifications)

        @Provides
        fun provideSetHideRecentActivity(settingsRepository: SettingsRepository): SetHideRecentActivity =
            SetHideRecentActivity(settingsRepository::setHideRecentActivity)

    }
}