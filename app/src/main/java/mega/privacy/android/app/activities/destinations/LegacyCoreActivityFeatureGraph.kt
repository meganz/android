package mega.privacy.android.app.activities.destinations

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.mediaplayer.legacyMediaPlayerScreen
import mega.privacy.android.app.nav.MediaPlayerIntentMapper
import mega.privacy.android.app.presentation.chat.navigation.chatLegacyDestination
import mega.privacy.android.app.presentation.chat.navigation.chatListLegacyDestination
import mega.privacy.android.app.presentation.contact.navigation.contactsLegacyDestination
import mega.privacy.android.app.presentation.filelink.legacyFileLinkScreen
import mega.privacy.android.app.presentation.folderlink.legacyFolderLinkScreen
import mega.privacy.android.app.presentation.imagepreview.legacyImageViewerScreen
import mega.privacy.android.app.presentation.pdfviewer.legacyPdfViewerScreen
import mega.privacy.android.app.presentation.search.navigation.searchLegacyDestination
import mega.privacy.android.app.presentation.settings.cookieSettingsNavigationDestination
import mega.privacy.android.app.presentation.settings.exportrecoverykey.legacyExportRecoveryKeyScreen
import mega.privacy.android.app.presentation.settings.settingsCameraUploadsNavigationDestination
import mega.privacy.android.app.presentation.testpassword.navigation.testPasswordLegacyDestination
import mega.privacy.android.app.textEditor.legacyTextEditorScreen
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class LegacyCoreActivityFeatureGraph(
    nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    mediaPlayerIntentMapper: MediaPlayerIntentMapper,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            overDiskQuotaPaywallWarning(navigationHandler::back)
            upgradeAccount(navigationHandler::back)
            myAccount(navigationHandler::back)
            achievement(navigationHandler::back)
            webDestinations(navigationHandler::back)
            cookieSettingsNavigationDestination(navigationHandler::back)
            settingsCameraUploadsNavigationDestination(navigationHandler::back)
            searchLegacyDestination(navigationHandler::back)
            contactsLegacyDestination(navigationHandler::back)
            chatLegacyDestination(navigationHandler::back)
            testPasswordLegacyDestination(navigationHandler::back)
            syncListDestination(navigationHandler::back)
            syncNewFolderDestination(navigationHandler::back)
            syncSelectStopBackupDestinationDestination(navigationHandler::back)
            legacyFileLinkScreen(navigationHandler::back)
            legacyExportRecoveryKeyScreen(navigationHandler::back)
            legacyFolderLinkScreen(navigationHandler::back)
            chatListLegacyDestination(navigationHandler::back)
            legacyAlbumCoverSelection(navigationHandler::returnResult)
            legacyAlbumGetLink(navigationHandler::back)
            legacyAlbumImport(navigationHandler::back)
            legacyPdfViewerScreen(navigationHandler::back, nodeContentUriIntentMapper)
            legacyImageViewerScreen(navigationHandler::back)
            legacyTextEditorScreen(navigationHandler::back)
            legacyMediaPlayerScreen(navigationHandler::back, mediaPlayerIntentMapper)
        }
}
