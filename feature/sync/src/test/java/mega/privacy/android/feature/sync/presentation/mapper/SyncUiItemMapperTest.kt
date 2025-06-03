package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.feature.sync.data.mapper.SyncStatusMapper
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncUiItemMapperTest {

    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper = mock()
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper = mock()
    private val syncStatusMapper: SyncStatusMapper = mock()
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase = mock()
    private val underTest = SyncUiItemMapper(
        deviceFolderUINodeErrorMessageMapper = deviceFolderUINodeErrorMessageMapper,
        backupInfoTypeIntMapper = backupInfoTypeIntMapper,
        syncStatusMapper = syncStatusMapper,
        getPathByDocumentContentUriUseCase = getPathByDocumentContentUriUseCase
    )

    private val folderPair = FolderPair(
        id = 3L,
        syncType = SyncType.TYPE_TWOWAY,
        pairName = "folderPair",
        localFolderPath = "content://com.android.externalstorage.documents/document/primary%3ADCIM",
        remoteFolder = RemoteFolder(id = NodeId(233L), name = "photos"),
        syncStatus = SyncStatus.SYNCING,
        syncError = null
    )
    private val folderPairsList = listOf(
        folderPair
    )

    @AfterEach
    fun tearDown() {
        reset(
            deviceFolderUINodeErrorMessageMapper,
            backupInfoTypeIntMapper,
            syncStatusMapper,
            getPathByDocumentContentUriUseCase
        )
    }

    @Test
    fun `test on correct conversion`() = runTest {
        val localPath = "/path/to/DCIM"
        whenever(deviceFolderUINodeErrorMessageMapper(folderPair.syncError)).thenReturn(null)
        whenever(getPathByDocumentContentUriUseCase(folderPair.localFolderPath)).thenReturn(
            localPath
        )
        val syncUiItems = listOf(
            SyncUiItem(
                id = 3L,
                syncType = SyncType.TYPE_TWOWAY,
                folderPairName = "folderPair",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = localPath,
                megaStoragePath = "photos",
                megaStorageNodeId = NodeId(233L),
                expanded = false,
                deviceStorageUri = UriPath("content://com.android.externalstorage.documents/document/primary%3ADCIM"),
            )
        )

        assertThat(underTest(folderPairsList)).isEqualTo(syncUiItems)
    }

    @Test
    fun `test on incorrect conversion`() = runTest {
        whenever(deviceFolderUINodeErrorMessageMapper(folderPair.syncError)).thenReturn(null)
        val localPath = "/path/to/DCIM"
        val syncUiItems = listOf(
            SyncUiItem(
                id = 4L,
                syncType = SyncType.TYPE_TWOWAY,
                folderPairName = "folderPair",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = localPath,
                megaStoragePath = "photos",
                megaStorageNodeId = NodeId(1234L),
                expanded = false
            )
        )

        assertThat(underTest(folderPairsList.map { it.copy(localFolderPath = localPath) }))
            .isNotEqualTo(syncUiItems)
        verifyNoInteractions(getPathByDocumentContentUriUseCase)
    }

    @Test
    fun `test when syncError is COULD_NOT_CREATE_IGNORE_FILE`() = runTest {
        val folderPairWithError =
            folderPair.copy(syncError = SyncError.COULD_NOT_CREATE_IGNORE_FILE)
        val expectedSyncUiItem = SyncUiItem(
            id = folderPairWithError.id,
            syncType = folderPairWithError.syncType,
            folderPairName = folderPairWithError.pairName,
            status = folderPairWithError.syncStatus,
            hasStalledIssues = false,
            deviceStoragePath = folderPairWithError.localFolderPath,
            deviceStorageUri = UriPath(folderPairWithError.localFolderPath),
            megaStoragePath = folderPairWithError.remoteFolder.name,
            megaStorageNodeId = folderPairWithError.remoteFolder.id,
            expanded = false,
            error = null,
            isLocalRootChangeNeeded = true
        )

        whenever(deviceFolderUINodeErrorMessageMapper(folderPairWithError.syncError)).thenReturn(
            null
        )

        assertThat(underTest(listOf(folderPairWithError))).isEqualTo(listOf(expectedSyncUiItem))
    }
}
