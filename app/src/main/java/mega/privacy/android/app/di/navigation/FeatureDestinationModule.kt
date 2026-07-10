package mega.privacy.android.app.di.navigation

import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.activities.destinations.LegacyCoreActivityFeatureGraph
import mega.privacy.android.app.appstate.content.destinations.FetchNodeProviderImpl
import mega.privacy.android.app.appstate.content.navigation.FetchNodeProvider
import mega.privacy.android.app.appstate.content.navigation.PermissionFeatureDestination
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.ads.AdsFreeIntroFeatureDestination
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.nav.MediaPlayerIntentMapper
import mega.privacy.android.app.presentation.documentscanner.navigation.LegacyScanDocumentDestination
import mega.privacy.android.app.presentation.documentscanner.navigation.SaveScannedDocumentsDestination
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactFeatureDestination
import mega.privacy.android.app.presentation.filestorage.FileStorageFeatureDestination
import mega.privacy.android.app.presentation.logout.LogoutFeatureDestination
import mega.privacy.android.app.presentation.meeting.navigation.MeetingFeatureDestination
import mega.privacy.android.app.presentation.notification.navigation.NotificationsFeatureDestination
import mega.privacy.android.app.presentation.psa.PsaFeatureDestinations
import mega.privacy.android.app.presentation.settings.SettingsCameraUploadsFeatureDestination
import mega.privacy.android.app.mediaplayer.AudioPlayerLaunchSourceHolder
import mega.privacy.android.app.mediaplayer.Nav3AudioPlayerRouteLauncher
import mega.privacy.android.app.mediaplayer.navigation.AudioPlayerFeatureDestination
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerLaunchSourceHolder
import mega.privacy.android.app.presentation.videoplayer.Nav3VideoPlayerRouteLauncher
import mega.privacy.android.app.presentation.videoplayer.navigation.VideoPlayerFeatureDestination
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserFeatureDestination
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.feature.documentscanner.navigation.ContinuousScanDestination
import mega.privacy.android.feature.pdfviewer.navigation.PdfViewerFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FeatureDestinationModule {

    @Provides
    @IntoSet
    fun provideLegacyCoreActivityFeatureDestination(
        nodeContentUriIntentMapper: NodeContentUriIntentMapper,
        mediaPlayerIntentMapper: MediaPlayerIntentMapper,
        nav3VideoPlayerRouteLauncher: Nav3VideoPlayerRouteLauncher,
        nav3AudioPlayerRouteLauncher: Nav3AudioPlayerRouteLauncher,
        megaChatRequestHandler: Lazy<MegaChatRequestHandler>,
        chatManagement: Lazy<ChatManagement>,
        setChatVideoInDeviceUseCase: Lazy<SetChatVideoInDeviceUseCase>,
        rtcAudioManagerGateway: Lazy<RTCAudioManagerGateway>,
        viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
        snackbarEventQueue: SnackbarEventQueue,
    ): FeatureDestination =
        LegacyCoreActivityFeatureGraph(
            nodeContentUriIntentMapper,
            mediaPlayerIntentMapper,
            nav3VideoPlayerRouteLauncher,
            nav3AudioPlayerRouteLauncher,
            megaChatRequestHandler,
            chatManagement,
            setChatVideoInDeviceUseCase,
            rtcAudioManagerGateway,
            viewTypeToNodeSourceTypeMapper,
            snackbarEventQueue,
        )

    @Provides
    @IntoSet
    fun provideFileContactFeatureDestination(): FeatureDestination = FileContactFeatureDestination()

    @Provides
    @IntoSet
    fun provideVideoPlayerFeatureDestination(
        launchSourceHolder: VideoPlayerLaunchSourceHolder,
    ): FeatureDestination = VideoPlayerFeatureDestination(launchSourceHolder)

    @Provides
    @IntoSet
    fun provideAudioPlayerFeatureDestination(
        launchSourceHolder: AudioPlayerLaunchSourceHolder,
    ): FeatureDestination = AudioPlayerFeatureDestination(launchSourceHolder)

    @Provides
    @IntoSet
    fun provideNotificationsFeatureDestination(): FeatureDestination =
        NotificationsFeatureDestination()

    @Provides
    @IntoSet
    fun provideLogoutFeatureDestination(): FeatureDestination =
        LogoutFeatureDestination()

    @Provides
    @IntoSet
    fun providePermissionFeatureDestination(): FeatureDestination =
        PermissionFeatureDestination()

    @Provides
    @IntoSet
    fun provideFileStorageFeatureDestination(): FeatureDestination = FileStorageFeatureDestination()

    @Provides
    @IntoSet
    fun provideZipBrowserFeatureDestination(): FeatureDestination = ZipBrowserFeatureDestination()

    @Provides
    @IntoSet
    fun providePsaFeatureDestination(): FeatureDestination = PsaFeatureDestinations()

    @Provides
    @IntoSet
    fun provideMeetingFeatureDestination(): FeatureDestination = MeetingFeatureDestination

    @Provides
    @IntoSet
    fun provideScanDestination(): FeatureDestination = SaveScannedDocumentsDestination()

    @Provides
    @IntoSet
    fun provideLegacyScanDocumentDestination(): FeatureDestination =
        LegacyScanDocumentDestination()

    @Provides
    @IntoSet
    fun provideSettingsCameraUploadsFeatureDestination(): FeatureDestination =
        SettingsCameraUploadsFeatureDestination()

    @Provides
    @IntoSet
    fun providePdfViewerFeatureDestination(): FeatureDestination = PdfViewerFeatureDestination

    @Provides
    @IntoSet
    fun provideContinuousScanDestination(): FeatureDestination =
        ContinuousScanDestination()

    @Provides
    @IntoSet
    fun provideAdsFreeIntroFeatureDestination(): FeatureDestination =
        AdsFreeIntroFeatureDestination()

    @Provides
    @Singleton
    fun provideFetchNodeProvider(): FetchNodeProvider = FetchNodeProviderImpl()
}
