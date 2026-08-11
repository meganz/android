package mega.privacy.android.app.di.appstate

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.appstate.global.initialisation.appcreate.AccountDefaultsInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.AnalyticsInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.ApiServerInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.CallObserverInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.CameraUploadsWorkerNotificationInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.ChatApiInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.CloudDriveDocumentProviderInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.CoilImageLoaderInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.CrashReportingInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.EmojiInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.FcmTopicInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.FolderApiSetupInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.GreeterInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.LoggingInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.MiscFlagsInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.NetworkStateInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.NotificationChannelsInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.PasscodeInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.RemoteConfigInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.SdkSetupInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.StaticContextInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.SyncMonitorInitialiser
import mega.privacy.android.app.appstate.global.initialisation.appcreate.ThemeInitialiser
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser

/**
 * Provides the app-create initialisers as a single explicitly ordered list.
 *
 * Ordering is part of the boot contract: critical units run synchronously in this order, so any
 * addition or reordering must be deliberate and reviewed.
 */
@Module
@InstallIn(SingletonComponent::class)
internal class AppCreateInitialisersModule {

    @Provides
    fun provideAppCreateInitialisers(
        miscFlagsInitialiser: MiscFlagsInitialiser,
        apiServerInitialiser: ApiServerInitialiser,
        accountDefaultsInitialiser: AccountDefaultsInitialiser,
        greeterInitialiser: GreeterInitialiser,
        networkStateInitialiser: NetworkStateInitialiser,
        fcmTopicInitialiser: FcmTopicInitialiser,
        emojiInitialiser: EmojiInitialiser,
        remoteConfigInitialiser: RemoteConfigInitialiser,
        cameraUploadsWorkerNotificationInitialiser: CameraUploadsWorkerNotificationInitialiser,
        syncMonitorInitialiser: SyncMonitorInitialiser,
        cloudDriveDocumentProviderInitialiser: CloudDriveDocumentProviderInitialiser,
    ): Set<@JvmSuppressWildcards AsyncAppCreateInitialiser> = setOf(
        miscFlagsInitialiser,
        apiServerInitialiser,
        accountDefaultsInitialiser,
        greeterInitialiser,
        fcmTopicInitialiser,
        networkStateInitialiser,
        emojiInitialiser,
        remoteConfigInitialiser,
        cameraUploadsWorkerNotificationInitialiser,
        syncMonitorInitialiser,
        cloudDriveDocumentProviderInitialiser,
    )

    @Provides
    fun provideSyncAppCreateInitialisers(
        loggingInitialiser: LoggingInitialiser,
        sdkSetupInitialiser: SdkSetupInitialiser,
        folderApiSetupInitialiser: FolderApiSetupInitialiser,
        crashReportingInitialiser: CrashReportingInitialiser,
        themeInitialiser: ThemeInitialiser,
        callObserverInitialiser: CallObserverInitialiser,
        chatApiInitialiser: ChatApiInitialiser,
        coilImageLoaderInitialiser: CoilImageLoaderInitialiser,
        staticContextInitialiser: StaticContextInitialiser,
        analyticsInitialiser: AnalyticsInitialiser,
        passcodeInitialiser: PasscodeInitialiser,
        notificationChannelsInitialiser: NotificationChannelsInitialiser,
    ): List<@JvmSuppressWildcards SynchronousAppCreateInitialiser> = listOf(
        loggingInitialiser,
        crashReportingInitialiser,
        sdkSetupInitialiser,
        folderApiSetupInitialiser,
        themeInitialiser,
        callObserverInitialiser,
        chatApiInitialiser,
        coilImageLoaderInitialiser,
        staticContextInitialiser,
        analyticsInitialiser,
        passcodeInitialiser,
        notificationChannelsInitialiser,
    )
}
