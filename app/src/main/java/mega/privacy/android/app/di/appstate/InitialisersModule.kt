package mega.privacy.android.app.di.appstate

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import mega.privacy.android.app.appstate.global.initialisation.postlogin.OnboardingPaymentInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.OnboardingPermissionInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.ReloadContactDatabaseInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PreLoginInitialiser
import mega.privacy.android.app.appstate.initialisation.postlogin.PurchaseReviewInitialiser
import mega.privacy.android.app.consent.initialiser.ConsentInitialiser
import mega.privacy.android.app.presentation.login.logoutdialog.RemoteLogoutInitialiser
import mega.privacy.android.app.sslverification.initialiser.SSLErrorMonitorInitialiser
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.logging.Logger
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase

@Module
@InstallIn(SingletonComponent::class)
class InitialisersModule {

    @Provides
    @IntoSet
    fun provideHistoricalProcessExitReasonsUseCaseInitialiser(getHistoricalProcessExitReasonsUseCase: GetHistoricalProcessExitReasonsUseCase): AppStartInitialiser =
        AppStartInitialiser { getHistoricalProcessExitReasonsUseCase() }

    @Provides
    @IntoSet
    fun provideResetChatSettingsUseCaseInitialiser(resetChatSettingsUseCase: ResetChatSettingsUseCase): AppStartInitialiser =
        AppStartInitialiser { resetChatSettingsUseCase() }

    @Provides
    @IntoSet
    fun provideDomainLoggerInitialiser(logger: Logger): AppStartInitialiser =
        AppStartInitialiser { Log.setLogger(logger) }

    @Provides
    @ElementsIntoSet
    fun providePreLoginInitialisers(): Set<PreLoginInitialiser> = emptySet()

    @Provides
    @IntoSet
    fun provideChatPostLoginInitialisers(useCase: InitialiseMegaChatUseCase): PostLoginInitialiser =
        PostLoginInitialiser { session, isFastLogin ->
            useCase(session)
        }

    @Provides
    @IntoSet
    fun providePurchaseReviewInitialiser(initialiser: PurchaseReviewInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideSslErrorMonitorAppStartInitialiser(initialiser: SSLErrorMonitorInitialiser): AppStartInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideConsentInitialiser(initialiser: ConsentInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideRemoteLogoutInitialiser(initialiser: RemoteLogoutInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideOnboardingPermissionInitialiser(initialiser: OnboardingPermissionInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideOnboardingPaymentInitialiser(initialiser: OnboardingPaymentInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideReloadContactDatabaseInitialiser(initialiser: ReloadContactDatabaseInitialiser): PostLoginInitialiser =
        initialiser
}