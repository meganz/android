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

    private fun syncTypeParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP),
    )
}
