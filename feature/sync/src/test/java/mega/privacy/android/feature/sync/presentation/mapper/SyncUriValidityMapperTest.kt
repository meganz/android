package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUriValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUriValidityResult
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncUriValidityMapperTest {

    private val getFolderPairsUseCase: GetFolderPairsUseCase = mock()
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase = mock()
    private val getLocalDCIMFolderPathUseCase: GetLocalDCIMFolderPathUseCase = mock()
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase = mock()
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase = mock()

    private lateinit var underTest: SyncUriValidityMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncUriValidityMapper(
            getFolderPairsUseCase = getFolderPairsUseCase,
            getPathByDocumentContentUriUseCase = getPathByDocumentContentUriUseCase,
            getLocalDCIMFolderPathUseCase = getLocalDCIMFolderPathUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase
        )
    }


    @AfterEach
    fun resetAndTearDown() {
        reset(
            getFolderPairsUseCase,
            getPathByDocumentContentUriUseCase,
            getLocalDCIMFolderPathUseCase,
        )
    }

    @Test
    fun `test that selecting DCIM folder shows snackbar`() = runTest {
        val documentUri = "content://storage/emulated/0/DCIM"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/PHOTOS"
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(dcimPath)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn(dcimPath)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(mediaUploadPath)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(cameraUploadsFolder)


        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
    }

    @Test
    fun `test that selecting Camera Uploads folder shows snackbar`() = runTest {
        val documentUri = "content://storage/emulated/0/DCIM_PHOTOS"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/PHOTOS"
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(dcimPath)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn(dcimPath)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(mediaUploadPath)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(cameraUploadsFolder)

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
    }

    @Test
    fun `test that selecting Media folder shows snackbar`() = runTest {
        val documentUri = "content://storage/emulated/0/PHOTOS"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/PHOTOS"
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(dcimPath)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn(dcimPath)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(mediaUploadPath)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(cameraUploadsFolder)

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that selecting already synced folder shows snackbar`(syncType: SyncType) = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"
        val dcimPath = "/storage/emulated/0/DCIM"
        val mediaUploadPath = "content://storage/emulated/0/SHARED_PHOTOS"
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn(dcimPath)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(mediaUploadPath)
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = syncType,
                pairName = "Pair Name",
                localFolderPath = "content://storage/emulated/0/Photos",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Remote Folder"),
                syncStatus = SyncStatus.SYNCED
            )
        )
        val snackbarMessage = when (syncType) {
            SyncType.TYPE_BACKUP -> sharedR.string.sync_local_device_folder_currently_backed_up_message
            else -> sharedR.string.sync_local_device_folder_currently_synced_message
        }
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(folderPath)
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(snackbarMessage)
    }

    @Test
    fun `test that selecting valid folder returns valid result`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/SHARED_PHOTOS"
        whenever(getPrimaryFolderPathUseCase()).thenReturn(cameraUploadsFolder)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(mediaUploadPath)
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(folderPath)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getFolderPairsUseCase()).thenReturn(emptyList())

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncUriValidityResult.ValidFolderSelected
        assertThat(validResult.localFolderUri.value).isEqualTo(documentUri)
        assertThat(validResult.folderName).isEqualTo("Photos")
    }

    @Test
    fun `test that invalid folder returns invalid result`() = runTest {
        val documentUri = "content://invalid/path"
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(null)

        val result = underTest(documentUri)

        assertThat(result).isEqualTo(SyncUriValidityResult.Invalid)
    }

    @Test
    fun `test determinePathRelationship with exact match`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Photos Sync",
                localFolderPath = "content://storage/emulated/0/Photos",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Photos"),
                syncStatus = SyncStatus.SYNCED
            )
        )

        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(folderPath)
        whenever(getPathByDocumentContentUriUseCase("content://storage/emulated/0/Photos")).thenReturn(
            folderPath
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.sync_local_device_folder_currently_synced_message)
    }

    @Test
    fun `test determinePathRelationship with external path as subfolder of local path`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos/Vacation"
        val externalPath = "/storage/emulated/0/Photos/Vacation"
        val localPath = "/storage/emulated/0/Photos"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Photos Sync",
                localFolderPath = "content://storage/emulated/0/Photos",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Photos"),
                syncStatus = SyncStatus.SYNCED
            )
        )

        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(externalPath)
        whenever(getPathByDocumentContentUriUseCase("content://storage/emulated/0/Photos")).thenReturn(
            localPath
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.general_sync_active_sync_below_path)
    }

    @Test
    fun `test determinePathRelationship with local path as subfolder of external path`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val externalPath = "/storage/emulated/0/Photos"
        val localPath = "/storage/emulated/0/Photos/Vacation"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Vacation Sync",
                localFolderPath = "content://storage/emulated/0/Photos/Vacation",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Vacation"),
                syncStatus = SyncStatus.SYNCED
            )
        )

        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(externalPath)
        whenever(getPathByDocumentContentUriUseCase("content://storage/emulated/0/Photos/Vacation")).thenReturn(
            localPath
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.general_sync_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder)
    }

    @Test
    fun `test determinePathRelationship handles trailing slashes correctly`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos/"
        val externalPath = "/storage/emulated/0/Photos/"
        val localPath = "/storage/emulated/0/Photos"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = SyncType.TYPE_BACKUP,
                pairName = "Photos Sync",
                localFolderPath = "content://storage/emulated/0/Photos",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Photos"),
                syncStatus = SyncStatus.SYNCED
            )
        )

        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(externalPath)
        whenever(getPathByDocumentContentUriUseCase("content://storage/emulated/0/Photos")).thenReturn(
            localPath
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncUriValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.sync_local_device_folder_currently_backed_up_message)
    }

    @Test
    fun `test determinePathRelationship prevents false positives with similar names`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos2"
        val externalPath = "/storage/emulated/0/Photos2"
        val localPath = "/storage/emulated/0/Photos"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Photos Sync",
                localFolderPath = "content://storage/emulated/0/Photos",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Photos"),
                syncStatus = SyncStatus.SYNCED
            )
        )

        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(externalPath)
        whenever(getPathByDocumentContentUriUseCase("content://storage/emulated/0/Photos")).thenReturn(
            localPath
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncUriValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncUriValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("Photos2")
    }

    private fun syncTypeParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP),
    )
}
