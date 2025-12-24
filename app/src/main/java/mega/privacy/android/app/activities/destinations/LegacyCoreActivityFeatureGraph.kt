package mega.privacy.android.app.activities.destinations

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.getLink.navigation.getLinkLegacyDestination
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.mediaplayer.legacyMediaPlayerScreen
import mega.privacy.android.app.meeting.activity.legacyMeetingScreen
import mega.privacy.android.app.meeting.activity.legacyWaitingRoomScreen
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.nav.MediaPlayerIntentMapper
import mega.privacy.android.app.presentation.chat.navigation.chatLegacyDestination
import mega.privacy.android.app.presentation.chat.navigation.chatListLegacyDestination
import mega.privacy.android.app.presentation.contact.authenticitycredendials.navigation.authenticityCredentialsLegacyDestination
import mega.privacy.android.app.presentation.contact.invite.navigation.inviteContactLegacyDestination
import mega.privacy.android.app.presentation.contact.navigation.contactsLegacyDestination
import mega.privacy.android.app.activities.navigation.fileInfoScreen
import mega.privacy.android.app.presentation.meeting.managechathistory.navigation.manageChatHistoryLegacyDestination
import mega.privacy.android.app.presentation.filelink.legacyFileLinkScreen
import mega.privacy.android.app.presentation.folderlink.legacyFolderLinkScreen
import mega.privacy.android.app.presentation.imagepreview.legacyImageViewerScreen
import mega.privacy.android.app.presentation.photos.mediadiscovery.navigation.mediaDiscoveryLegacyDestination
import mega.privacy.android.app.presentation.documentscanner.navigation.saveScannedDocumentsLegacyDestination
import mega.privacy.android.app.presentation.pdfviewer.legacyPdfViewerScreen
import mega.privacy.android.app.presentation.search.navigation.searchLegacyDestination
import mega.privacy.android.app.presentation.settings.cookieSettingsNavigationDestination
import mega.privacy.android.app.presentation.settings.exportrecoverykey.legacyExportRecoveryKeyScreen
import mega.privacy.android.app.presentation.settings.settingsCameraUploadsNavigationDestination
import mega.privacy.android.app.presentation.testpassword.navigation.testPasswordLegacyDestination
import mega.privacy.android.app.presentation.videosection.legacyVideoToPlaylistDestination
import mega.privacy.android.app.presentation.videosection.videoSectionLegacyDestination
import mega.privacy.android.app.textEditor.legacyTextEditorScreen
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class LegacyCoreActivityFeatureGraph(
    nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    mediaPlayerIntentMapper: MediaPlayerIntentMapper,
    megaChatRequestHandler: MegaChatRequestHandler,
    chatManagement: ChatManagement,
    setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    rtcAudioManagerGateway: RTCAudioManagerGateway,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            overDiskQuotaPaywallWarning(navigationHandler::back)
            upgradeAccount(navigationHandler::remove)
            myAccount(navigationHandler::back)
            achievement(navigationHandler::back)
            webDestinations(navigationHandler::back)
            cookieSettingsNavigationDestination(navigationHandler::back)
            settingsCameraUploadsNavigationDestination(navigationHandler::back)
            searchLegacyDestination(navigationHandler::back)
            contactsLegacyDestination(navigationHandler::back)
            inviteContactLegacyDestination(navigationHandler::back)
            authenticityCredentialsLegacyDestination(navigationHandler::back)
            chatLegacyDestination(navigationHandler::back)
            manageChatHistoryLegacyDestination(navigationHandler::back)
            testPasswordLegacyDestination(navigationHandler::back)
            syncListDestination(navigationHandler::back)
            syncNewFolderDestination(navigationHandler::back)
            syncSelectStopBackupDestinationDestination(navigationHandler::back)
            legacyFileLinkScreen(navigationHandler::back)
            legacyExportRecoveryKeyScreen(navigationHandler::back)
            legacyFolderLinkScreen(navigationHandler::back)
            getLinkLegacyDestination(navigationHandler::back)
            chatListLegacyDestination(navigationHandler::back)
            legacyAlbumCoverSelection(navigationHandler::returnResult)
            legacyAlbumPhotosSelection(navigationHandler::back, navigationHandler::returnResult)
            legacyAlbumGetLink(navigationHandler::back)
            legacyAlbumImport(navigationHandler::back)
            legacyPdfViewerScreen(navigationHandler::back, nodeContentUriIntentMapper)
            legacyImageViewerScreen(navigationHandler::back)
            legacyTextEditorScreen(navigationHandler::back)
            legacyMediaPlayerScreen(navigationHandler::back, mediaPlayerIntentMapper)
            videoSectionLegacyDestination(navigationHandler::back)
            legacyAlbumContentPreview(navigationHandler::back)
            legacyMediaTimelinePhotoPreview(navigationHandler::back)
            legacyAddToAlbumActivityNavKey(navigationHandler::returnResult)
            legacyMeetingScreen(
                navigationHandler::back,
                megaChatRequestHandler,
                chatManagement,
                setChatVideoInDeviceUseCase,
                rtcAudioManagerGateway
            )
            legacyWaitingRoomScreen(navigationHandler::back, megaChatRequestHandler, chatManagement)
            legacyPhotosSearch(navigationHandler::back, navigationHandler::returnResult)
            legacySettingsCameraUploadsActivityNavKey(navigationHandler::back)
            fileInfoScreen(navigationHandler::back)
            saveScannedDocumentsLegacyDestination(navigationHandler::back)
            mediaDiscoveryLegacyDestination(navigationHandler::back)
            legacyVideoToPlaylistDestination(
                navigationHandler::back,
                navigationHandler::returnResult
            )
        }
}
