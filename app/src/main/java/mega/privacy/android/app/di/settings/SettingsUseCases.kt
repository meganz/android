package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

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
    abstract fun bindIsCameraSyncEnabled(useCase: DefaultIsCameraSyncEnabled): IsCameraSyncEnabled

    @Binds
    abstract fun bindIsMultiFactorAuthAvailable(useCase: DefaultIsMultiFactorAuthAvailable): IsMultiFactorAuthAvailable

    @Binds
    abstract fun bindFetchAutoAcceptQRLinks(useCase: DefaultFetchAutoAcceptQRLinks): FetchAutoAcceptQRLinks

    @Binds
    abstract fun bindStartScreen(useCase: DefaultGetStartScreen): GetStartScreen

    @Binds
    abstract fun bindIsHideRecentActivityEnabled(useCase: DefaultIsHideRecentActivityEnabled): IsHideRecentActivityEnabled

    @Binds
    abstract fun bindToggleAutoAcceptQRLinks(useCase: DefaultToggleAutoAcceptQRLinks): ToggleAutoAcceptQRLinks

    @Binds
    abstract fun bindRequestAccountDeletion(useCase: DefaultRequestAccountDeletion): RequestAccountDeletion

    @Binds
    abstract fun bindIsChatLoggedIn(useCase: DefaultIsChatLoggedIn): IsChatLoggedIn

    @Binds
    abstract fun bindFetchMultiFactorAuthSetting(useCase: DefaultFetchMultiFactorAuthSetting): FetchMultiFactorAuthSetting

    @Binds
    abstract fun bindMonitorAutoAcceptQRLinks(implementation: DefaultMonitorAutoAcceptQRLinks): MonitorAutoAcceptQRLinks


}