package mega.privacy.android.navigation.destination

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Every NavKey that carries a link, key, file or folder name, path, uri or free text is logged
 * through its toString(), so that representation must never contain those values.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NavKeyToStringTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("sensitiveNavKeys")
    fun `test that toString does not expose the sensitive value`(
        name: String,
        navKey: Any,
    ) {
        val actual = navKey.toString()

        assertThat(actual).doesNotContain(SENSITIVE)
        assertThat(actual).startsWith(name)
    }

    private fun sensitiveNavKeys(): Stream<Arguments> = listOf(
        LoginNavKey(action = "action", link = SENSITIVE),
        ParkAccountNavKey(link = SENSITIVE),
        CloudDriveNavKey(nodeName = SENSITIVE, highlightedNodeNames = listOf(SENSITIVE)),
        OfflineNavKey(title = SENSITIVE, path = SENSITIVE, highlightedFiles = SENSITIVE),
        CloudDriveMediaDiscoveryNavKey(folderId = 1L, folderName = SENSITIVE),
        SearchNavKey(parentHandle = 1L, folderLinkUrl = SENSITIVE),
        FolderLinkNavKey(uriString = SENSITIVE),
        FileLinkNavKey(uriString = SENSITIVE),
        ShareFilesToMegaNavKey(shareUris = listOf(UriPath(SENSITIVE))),
        ShareTextToMegaNavKey(text = SENSITIVE, subject = SENSITIVE, email = SENSITIVE),
        UploadScannedDocumentNavKey(
            uriPath = UriPath(SENSITIVE),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        ),
        SelectStopBackupDestinationNavKey(folderName = SENSITIVE),
        NodesExplorerNavKey(
            nodeId = NodeId(1L),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            explorerMode = ExplorerMode.ShareFilesToMega,
            startNavKey = ShareFilesToMegaNavKey(shareUris = listOf(UriPath(SENSITIVE))),
            shareUris = listOf(UriPath(SENSITIVE)),
        ),
        DeepLinksDialogNavKey(deepLink = SENSITIVE),
        DeepLinksAfterFetchNodesDialogNavKey(
            deepLink = SENSITIVE,
            regexPatternType = RegexPatternType.RESTRICTED,
        ),
        SaveScannedDocumentsNavKey(scanPdfUri = SENSITIVE, scanSoloImageUri = SENSITIVE),
        DiscardScanWarningDialogNavKey(
            hasMultipleScans = false,
            startNavKey = UploadScannedDocumentNavKey(
                uriPath = UriPath(SENSITIVE),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            ),
        ),
        MediaDiscoveryNavKey(nodeHandle = 1L, nodeName = SENSITIVE),
        RecentsBucketScreenNavKey(
            identifier = SENSITIVE,
            isMediaBucket = false,
            folderName = SENSITIVE,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            timestamp = 1L,
            fileCount = 1,
        ),
        HomeScreensNavKey(
            root = ChatListNavKey(),
            destinations = listOf(DeepLinksDialogNavKey(deepLink = SENSITIVE)),
        ),
        PdfViewerNavKey(contentUri = SENSITIVE, title = SENSITIVE, publicLinkUrl = SENSITIVE),
        LegacyMeetingNavKey(
            chatId = 1L,
            meetingInfo = MeetingNavKeyInfo.JoinAsGuest(meetingName = SENSITIVE, link = SENSITIVE),
        ),
        LegacyMeetingNavKey(
            chatId = 1L,
            meetingInfo = MeetingNavKeyInfo.JoinInProgressCall(
                meetingName = SENSITIVE,
                link = SENSITIVE,
            ),
        ),
        LegacyMeetingNavKey(
            chatId = 1L,
            meetingInfo = MeetingNavKeyInfo.RejoinInProgressCall(
                meetingName = SENSITIVE,
                publicChatHandle = 2L,
                link = SENSITIVE,
            ),
        ),
        LegacyWaitingRoomNavKey(
            chatId = 1L,
            waitingRoomInfo = WaitingRoomNavKeyInfo.JoinAsGuest(link = SENSITIVE),
        ),
        LegacyWaitingRoomNavKey(
            chatId = 1L,
            waitingRoomInfo = WaitingRoomNavKeyInfo.JoinWaitingRoom(link = SENSITIVE),
        ),
        SelectVideosForPlaylistNavKey(nodeName = SENSITIVE),
        AlbumImportNavKey(link = SENSITIVE),
        SelectVideosSearchNavKey(nodeName = SENSITIVE),
        MyAccountNavKey(link = SENSITIVE),
        WebSiteNavKey(url = SENSITIVE),
        ChatNavKey(chatId = 1L, link = SENSITIVE, snackbarText = SENSITIVE),
        FileContactInfoNavKey(folderHandle = 1L, folderName = SENSITIVE),
        SyncNewFolderNavKey(remoteFolderName = SENSITIVE),
        SyncSelectStopBackupDestinationNavKey(folderName = SENSITIVE),
        FileStorageNavKey(uriPath = SENSITIVE, highlightedFiles = listOf(SENSITIVE)),
        LegacyZipBrowserNavKey(zipFilePath = SENSITIVE),
        LegacyFolderLinkNavKey(uriString = SENSITIVE),
        AlbumContentPreviewNavKey(
            albumId = 1L,
            photoId = 2L,
            albumType = "type",
            sortType = "sort",
            title = SENSITIVE,
        ),
        AlbumImportPreviewNavKey(photoId = 1L, albumLink = SENSITIVE),
        LegacyPdfViewerNavKey(
            nodeHandle = 1L,
            nodeContentUri = NodeContentUri.RemoteContentUri(SENSITIVE, false),
            mimeType = "application/pdf",
        ),
        LegacyImageViewerNavKey(
            nodeHandle = 1L,
            parentNodeHandle = 2L,
            url = SENSITIVE,
            bucketId = SENSITIVE,
        ),
        LegacyTextEditorNavKey(
            fileName = SENSITIVE,
            localPath = SENSITIVE,
            publicUrl = SENSITIVE,
            serializedNode = SENSITIVE,
            folderLinkUrl = SENSITIVE,
        ),
        LegacyMediaPlayerNavKey(
            nodeHandle = 1L,
            nodeContentUri = NodeContentUri.RemoteContentUri(SENSITIVE, false),
            fileName = SENSITIVE,
            parentHandle = 2L,
            fileHandle = 3L,
            fileTypeInfo = UnknownFileTypeInfo(mimeType = "video/mp4", extension = "mp4"),
            mediaQueueTitle = SENSITIVE,
            collectionTitle = SENSITIVE,
            publicLinkUrl = SENSITIVE,
            localFilePath = SENSITIVE,
        ),
        MediaTimelinePhotoPreviewNavKey(
            id = 1L,
            sortType = "sort",
            filterType = "filter",
            mediaSource = "source",
            anchorThumbnailPath = SENSITIVE,
        ),
        FileExplorerNavKey(action = "action", shareUri = SENSITIVE),
        AuthenticityCredentialsNavKey(email = SENSITIVE, isIncomingShares = false),
        CannotVerifyContactDialogNavKey(email = SENSITIVE),
        ContactInfoNavKey(email = SENSITIVE),
        ContactSharedFoldersNavKey(email = SENSITIVE),
        CreateAccountNavKey(initialEmail = SENSITIVE),
        ManageChatHistoryNavKey(chatId = 1L, email = SENSITIVE),
    ).map { Arguments.of(it::class.simpleName, it) }.stream()

    private companion object {
        const val SENSITIVE = "https://mega.nz/file/HANDLE#CANARY_NODE_KEY/canary-name.txt"
    }
}
