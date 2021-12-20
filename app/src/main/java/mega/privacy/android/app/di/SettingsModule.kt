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
    abstract fun bindRefreshUserAccount(
        useCase: DefaultRefreshUserAccount
    ): RefreshUserAccount

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
        useCase: DefaultFetchContactLinksOption
    ): FetchContactLinksOption

    @Binds
    abstract fun bindPerformMultiFactorAuthCheck(
        useCase: DefaultPerformMultiFactorAuthCheck
    ): PerformMultiFactorAuthCheck

    @Binds
    abstract fun bindGetStartScreen(
        useCase: DefaultGetStartScreen
    ): GetStartScreen

    @Binds
    abstract fun bindShouldHideRecentActivity(
        useCase: DefaultShouldHideRecentActivity
    ): ShouldHideRecentActivity

}