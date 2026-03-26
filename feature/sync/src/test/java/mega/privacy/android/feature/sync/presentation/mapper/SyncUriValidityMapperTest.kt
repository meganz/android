package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUriValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncValidityResult
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
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    private lateinit var underTest: SyncUriValidityMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncUriValidityMapper(
            getFolderPairsUseCase = getFolderPairsUseCase,
            getPathByDocumentContentUriUseCase = getPathByDocumentContentUriUseCase,
            getLocalDCIMFolderPathUseCase = getLocalDCIMFolderPathUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(
            getFolderPairsUseCase,
            getPathByDocumentContentUriUseCase,
            getLocalDCIMFolderPathUseCase,
            getSecondaryFolderPathUseCase,
            getPrimaryFolderPathUseCase,
            isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase,
            getFeatureFlagValueUseCase
        )
    }

    /**
     * Common stub method to reduce boilerplate in tests.
     * Provides default values that can be overridden for specific test cases.
     */
    private suspend fun stubCommonMocks(
        documentUri: String,
        documentPath: String? = null,
        dcimPath: String = "",
        cameraUploadsPath: String = "",
        mediaUploadPath: String = "",
        isCameraUploadsEnabled: Boolean = false,
        isMediaUploadsEnabled: Boolean = false,
        folderPairs: List<FolderPair> = emptyList(),
        folderPathOverrides: Map<String, String?> = emptyMap(),
        isDCIMSelectionEnabled: Boolean = false,
    ) {
        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(documentPath)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn(dcimPath)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(cameraUploadsPath)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(mediaUploadPath)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(isMediaUploadsEnabled)
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(isDCIMSelectionEnabled)

        // Override path mappings for specific URIs (useful for folder pair paths)
        folderPathOverrides.forEach { (uri, path) ->
            whenever(getPathByDocumentContentUriUseCase(uri)).thenReturn(path)
        }
    }

    @Test
    fun `test that selecting DCIM folder shows snackbar when isDCIMSelectionEnabled is false`() =
        runTest {
        val documentUri = "content://storage/emulated/0/DCIM"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/PHOTOS"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = dcimPath,
            dcimPath = dcimPath,
            cameraUploadsPath = cameraUploadsFolder,
            mediaUploadPath = mediaUploadPath,
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
    }

    @Test
    fun `test that selecting Camera Uploads folder shows snackbar`() = runTest {
        val documentUri = "content://storage/emulated/0/DCIM_PHOTOS"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/PHOTOS"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = dcimPath,
            dcimPath = dcimPath,
            cameraUploadsPath = cameraUploadsFolder,
            mediaUploadPath = mediaUploadPath,
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
    }

    @Test
    fun `test that selecting Media folder shows snackbar`() = runTest {
        val documentUri = "content://storage/emulated/0/PHOTOS"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/PHOTOS"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = dcimPath,
            dcimPath = dcimPath,
            cameraUploadsPath = cameraUploadsFolder,
            mediaUploadPath = mediaUploadPath,
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message)
    }

    @ParameterizedTest(name = "Sync type: {0}")
    @MethodSource("syncTypeParameters")
    fun `test that selecting already synced folder shows snackbar`(syncType: SyncType) = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"
        val dcimPath = "/storage/emulated/0/DCIM"
        val cameraUploadsPath = "/emulated/0/SHARED_PHOTOS"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = syncType,
                pairName = "Pair Name",
                localFolderPath = documentUri,
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Remote Folder"),
                syncStatus = SyncStatus.SYNCED
            )
        )
        val snackbarMessage = when (syncType) {
            SyncType.TYPE_BACKUP -> sharedR.string.sync_local_device_folder_currently_backed_up_message
            else -> sharedR.string.sync_local_device_folder_currently_synced_message
        }

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath,
            dcimPath = dcimPath,
            cameraUploadsPath = cameraUploadsPath,
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true,
            folderPairs = folderPairs
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId).isEqualTo(snackbarMessage)
    }

    @Test
    fun `test that selecting valid folder returns valid result`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"
        val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
        val mediaUploadPath = "content://storage/emulated/0/SHARED_PHOTOS"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath,
            cameraUploadsPath = cameraUploadsFolder,
            mediaUploadPath = mediaUploadPath,
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.localFolderUri.value).isEqualTo(documentUri)
        assertThat(validResult.folderName).isEqualTo("Photos")
    }

    @Test
    fun `test that invalid folder returns invalid result`() = runTest {
        val documentUri = "content://invalid/path"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = null
        )

        val result = underTest(documentUri)

        assertThat(result).isEqualTo(SyncValidityResult.Invalid)
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

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath,
            folderPairs = folderPairs,
            folderPathOverrides = mapOf("content://storage/emulated/0/Photos" to folderPath)
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
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

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = externalPath,
            folderPairs = folderPairs,
            folderPathOverrides = mapOf("content://storage/emulated/0/Photos" to localPath)
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
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

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = externalPath,
            folderPairs = folderPairs,
            folderPathOverrides = mapOf("content://storage/emulated/0/Photos/Vacation" to localPath)
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
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

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = externalPath,
            folderPairs = folderPairs,
            folderPathOverrides = mapOf("content://storage/emulated/0/Photos" to localPath)
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
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

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = externalPath,
            folderPairs = folderPairs,
            folderPathOverrides = mapOf("content://storage/emulated/0/Photos" to localPath)
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("Photos2")
    }

    @Test
    fun `test that selecting camera uploads folder when camera uploads disabled returns valid result`() =
        runTest {
            val documentUri = "content://storage/emulated/0/DCIM_PHOTOS"
            val folderPath = "/storage/emulated/0/DCIM_PHOTOS"
            val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
            val mediaUploadPath = "content://storage/emulated/0/SHARED_PHOTOS"

            stubCommonMocks(
                documentUri = documentUri,
                documentPath = folderPath,
                cameraUploadsPath = cameraUploadsFolder,
                mediaUploadPath = mediaUploadPath,
                isCameraUploadsEnabled = false,
                isMediaUploadsEnabled = true
            )

            val result = underTest(documentUri)

            assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
            val validResult = result as SyncValidityResult.ValidFolderSelected
            assertThat(validResult.folderName).isEqualTo("DCIM_PHOTOS")
        }

    @Test
    fun `test that selecting media uploads folder when media uploads disabled returns valid result`() =
        runTest {
            val documentUri = "content://storage/emulated/0/PHOTOS"
            val folderPath = "/storage/emulated/0/PHOTOS"
            val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
            val mediaUploadPath = "content://storage/emulated/0/PHOTOS"

            stubCommonMocks(
                documentUri = documentUri,
                documentPath = folderPath,
                cameraUploadsPath = cameraUploadsFolder,
                mediaUploadPath = mediaUploadPath,
                isCameraUploadsEnabled = true,
                isMediaUploadsEnabled = false
            )

            val result = underTest(documentUri)

            assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
            val validResult = result as SyncValidityResult.ValidFolderSelected
            assertThat(validResult.folderName).isEqualTo("PHOTOS")
        }

    @Test
    fun `test that selecting camera uploads folder when both uploads disabled returns valid result`() =
        runTest {
            val documentUri = "content://storage/emulated/0/DCIM_PHOTOS"
            val folderPath = "/storage/emulated/0/DCIM_PHOTOS"
            val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
            val mediaUploadPath = "content://storage/emulated/0/PHOTOS"

            stubCommonMocks(
                documentUri = documentUri,
                documentPath = folderPath,
                cameraUploadsPath = cameraUploadsFolder,
                mediaUploadPath = mediaUploadPath
            )

            val result = underTest(documentUri)

            assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
            val validResult = result as SyncValidityResult.ValidFolderSelected
            assertThat(validResult.folderName).isEqualTo("DCIM_PHOTOS")
        }

    @Test
    fun `test that selecting media uploads folder when both uploads disabled returns valid result`() =
        runTest {
            val documentUri = "content://storage/emulated/0/PHOTOS"
            val folderPath = "/storage/emulated/0/PHOTOS"
            val cameraUploadsFolder = "/storage/emulated/0/DCIM_PHOTOS"
            val mediaUploadPath = "content://storage/emulated/0/PHOTOS"

            stubCommonMocks(
                documentUri = documentUri,
                documentPath = folderPath,
                cameraUploadsPath = cameraUploadsFolder,
                mediaUploadPath = mediaUploadPath
            )

            val result = underTest(documentUri)

            assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
            val validResult = result as SyncValidityResult.ValidFolderSelected
            assertThat(validResult.folderName).isEqualTo("PHOTOS")
        }

    @Test
    fun `test that empty folder path returns invalid result`() = runTest {
        val documentUri = "content://storage/emulated/0/"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = ""
        )

        val result = underTest(documentUri)

        assertThat(result).isEqualTo(SyncValidityResult.Invalid)
    }

    @Test
    fun `test that folder path with only separator returns invalid result`() = runTest {
        val documentUri = "content://storage/emulated/0/"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = "/"
        )

        val result = underTest(documentUri)

        assertThat(result).isEqualTo(SyncValidityResult.Invalid)
    }

    @Test
    fun `test that folder path with trailing slash is handled correctly`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos/"
        val folderPath = "/storage/emulated/0/Photos/"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("Photos")
    }

    @Test
    fun `test that folder path with multiple trailing slashes is handled correctly`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos///"
        val folderPath = "/storage/emulated/0/Photos///"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("Photos")
    }

    @Test
    fun `test that folder path with spaces in name is handled correctly`() = runTest {
        val documentUri = "content://storage/emulated/0/My Photos/"
        val folderPath = "/storage/emulated/0/My Photos/"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("My Photos")
    }

    @Test
    fun `test that folder path with special characters is handled correctly`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos-2024/"
        val folderPath = "/storage/emulated/0/Photos-2024/"

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("Photos-2024")
    }

    @Test
    fun `test that exception in getPathByDocumentContentUriUseCase returns invalid result`() =
        runTest {
            val documentUri = "content://storage/emulated/0/Photos"
            whenever(getPathByDocumentContentUriUseCase(documentUri)).thenThrow(RuntimeException("Test exception"))

            val result = underTest(documentUri)

            assertThat(result).isEqualTo(SyncValidityResult.Invalid)
        }

    @Test
    fun `test that exception in getFolderPairsUseCase returns invalid result`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"

        whenever(getPathByDocumentContentUriUseCase(documentUri)).thenReturn(folderPath)
        whenever(getLocalDCIMFolderPathUseCase()).thenReturn("")
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")
        whenever(getFolderPairsUseCase()).thenThrow(RuntimeException("Test exception"))
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)

        val result = underTest(documentUri)

        assertThat(result).isEqualTo(SyncValidityResult.Invalid)
    }

    @Test
    fun `test that folder pair with null local path is skipped`() = runTest {
        val documentUri = "content://storage/emulated/0/Photos"
        val folderPath = "/storage/emulated/0/Photos"
        val folderPairs = listOf(
            FolderPair(
                id = 1234L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Photos Sync",
                localFolderPath = "content://invalid/path",
                remoteFolder = RemoteFolder(id = NodeId(5678L), name = "Photos"),
                syncStatus = SyncStatus.SYNCED
            )
        )

        stubCommonMocks(
            documentUri = documentUri,
            documentPath = folderPath,
            folderPairs = folderPairs,
            folderPathOverrides = mapOf("content://invalid/path" to null)
        )

        val result = underTest(documentUri)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        val validResult = result as SyncValidityResult.ValidFolderSelected
        assertThat(validResult.folderName).isEqualTo("Photos")
    }


    private fun syncTypeParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP),
    )
}
