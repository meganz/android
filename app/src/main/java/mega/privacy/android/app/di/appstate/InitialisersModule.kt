package mega.privacy.android.app.di.appstate

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.appstate.global.initialisation.postlogin.BusinessAccountExpiredInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.CameraUploadsSyncHandlesUpdaterInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.CheckBusinessStatusInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.ClearCompletedTransfersCacheInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.DeleteOldestCompletedTransfersInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.Enable2FAInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.MeetingEventsPostLoginInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.MonitorTransferEventsInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.NotificationTopicsInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.OfflineSyncPostLoginInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.OnboardingPaymentInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.OnboardingPermissionInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.PsaInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.PurchaseResultInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.PurchaseReviewInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.PushTokenPostLoginInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.ReloadContactDatabaseInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.SecurityUpgradeInitialiser
import mega.privacy.android.app.appstate.global.initialisation.postlogin.SetupLegacyViewsInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.StartCameraUploadsAfterStorageStateEventInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.StartTransferWorkerInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.UpdateActiveTransfersInitializer
import mega.privacy.android.app.appstate.global.initialisation.postlogin.WhatsNewInitializer
import mega.privacy.android.app.consent.initialiser.ConsentInitialiser
import mega.privacy.android.app.listeners.global.initialisers.ReloadEventInitialiser
import mega.privacy.android.app.presentation.login.logoutdialog.RemoteLogoutInitialiser
import mega.privacy.android.app.sslverification.initialiser.SSLErrorMonitorInitialiser
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.logging.Logger
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction

@Module
@InstallIn(SingletonComponent::class)
class InitialisersModule {

    @Provides
    @IntoSet
    fun provideHistoricalProcessExitReasonsUseCaseInitialiser(getHistoricalProcessExitReasonsUseCase: GetHistoricalProcessExitReasonsUseCase): AppStartInitialiser =
        AppStartInitialiserAction { getHistoricalProcessExitReasonsUseCase() }

    @Provides
    @IntoSet
    fun provideResetChatSettingsUseCaseInitialiser(resetChatSettingsUseCase: ResetChatSettingsUseCase): AppStartInitialiser =
        AppStartInitialiserAction { resetChatSettingsUseCase() }

    @Provides
    @IntoSet
    fun provideDomainLoggerInitialiser(logger: Logger): AppStartInitialiser =
        AppStartInitialiserAction { Log.setLogger(logger) }

    @Provides
    @IntoSet
    fun provideChatPostLoginInitialisers(useCase: InitialiseMegaChatUseCase): PostLoginInitialiser =
        PostLoginInitialiserAction { session, isFastLogin ->
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
    fun providePurchaseResultInitialiser(initialiser: PurchaseResultInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideBusinessAccountExpiredInitialiser(initialiser: BusinessAccountExpiredInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideCheckBusinessStatusInitialiser(initialiser: CheckBusinessStatusInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideSecurityUpgradeInitialiser(initialiser: SecurityUpgradeInitialiser): PostLoginInitialiser =
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
    fun providePushTokenPostLoginInitialiser(initialiser: PushTokenPostLoginInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideReloadContactDatabaseInitialiser(initialiser: ReloadContactDatabaseInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun providePsaInitialiser(initialiser: PsaInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideMeetingEventsPostLoginInitialiser(initialiser: MeetingEventsPostLoginInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideReloadEventMonitorAppStartInitialiser(initialiser: ReloadEventInitialiser): AppStartInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideNotificationTopicsInitializer(initialiser: NotificationTopicsInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideEnable2FAInitialiser(initialiser: Enable2FAInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideMonitorUserUpdatesAndEstablishCameraUploadsSyncHandlesInitializer(initialiser: CameraUploadsSyncHandlesUpdaterInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideStartCameraUploadsAfterStorageStateEventInitializer(initialiser: StartCameraUploadsAfterStorageStateEventInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideOfflineSyncPostLoginInitialiser(initialiser: OfflineSyncPostLoginInitialiser): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideDeleteOldestCompletedTransfersInitializer(initialiser: DeleteOldestCompletedTransfersInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideSetupLegacyViewsInitializer(initialiser: SetupLegacyViewsInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideStartTransferWorkerInitializer(initialiser: StartTransferWorkerInitializer): AppStartInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideMonitorTransferEventsInitializer(initialiser: MonitorTransferEventsInitializer): AppStartInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideUpdateActiveTransfersInitializer(initialiser: UpdateActiveTransfersInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideClearCompletedTransfersCacheInitializer(initialiser: ClearCompletedTransfersCacheInitializer): PostLoginInitialiser =
        initialiser

    @Provides
    @IntoSet
    fun provideWhatsNewInitializer(initialiser: WhatsNewInitializer): PostLoginInitialiser =
        initialiser
}
