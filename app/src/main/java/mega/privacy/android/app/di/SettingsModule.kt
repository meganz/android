package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindGetAccountDetails(
        useCase: DefaultGetAccountDetails
    ): GetAccountDetails

    @Binds
    abstract fun bindCanDeleteAccount(
        useCase: DefaultCanDeleteAccount
    ): CanDeleteAccount

    @Binds
    abstract fun bindRefreshPasscodeLockPreference(
        useCase: DefaultRefreshPasscodeLockPreference
    ): RefreshPasscodeLockPreference

    @Binds
    abstract fun bindIsLoggingEnabled(
        useCase: DefaultIsLoggingEnabled
    ): IsLoggingEnabled

    @Binds
    abstract fun bindIsChatLoggingEnabled(
        useCase: DefaultIsChatLoggingEnabled
    ): IsChatLoggingEnabled

    @Binds
    abstract fun bindIsCameraSyncEnabled(
        useCase: DefaultIsCameraSyncEnabled
    ): IsCameraSyncEnabled

    @Binds
    abstract fun bindRootNodeExists(
        useCase: DefaultRootNodeExists
    ): RootNodeExists

    @Binds
    abstract fun bindIsMultiFactorAuthAvailable(
        useCase: DefaultIsMultiFactorAuthAvailable
    ): IsMultiFactorAuthAvailable

    @Binds
    abstract fun bindFetchContactLinksOption(
        useCase: DefaultFetchAutoAcceptQRLinks
    ): FetchAutoAcceptQRLinks

    @Binds
    abstract fun bindFetchMultiFactorAuthSetting(
        useCase: DefaultFetchMultiFactorAuthSetting
    ): FetchMultiFactorAuthSetting

    @Binds
    abstract fun bindGetStartScreen(
        useCase: DefaultGetStartScreen
    ): GetStartScreen

    @Binds
    abstract fun bindShouldHideRecentActivity(
        useCase: DefaultIsHideRecentActivityEnabled
    ): IsHideRecentActivityEnabled

    @Binds
    abstract fun bindToggleAutoAcceptQRLinks(
        useCase: DefaultToggleAutoAcceptQRLinks
    ): ToggleAutoAcceptQRLinks

    @Binds
    abstract fun bindIsOnline(
        useCase: DefaultIsOnline
    ): IsOnline

    @Binds
    abstract fun bindRequestAccountDeletion(
        useCase: DefaultRequestAccountDeletion
    ): RequestAccountDeletion

    @Binds
    abstract fun bindIsChatLoggedIn(
        useCase: DefaultIsChatLoggedIn
    ): IsChatLoggedIn

    @Binds
    abstract fun bindSetLoggingEnabled(
        useCase: DefaultSetLoggingEnabled
    ): SetLoggingEnabled

    @Binds
    abstract fun bindSetChatLoggingEnabled(
        useCase: DefaultSetChatLoggingEnabled
    ): SetChatLoggingEnabled

    @Binds
    abstract fun bindGetFolderVersionInfo(
        implementation: DefaultGetFolderVersionInfo
    ): GetFolderVersionInfo

}