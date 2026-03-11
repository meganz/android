package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.camerauploads.HasLocalFolderConflictWithSyncUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [HasLocalFolderConflictWithSyncUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HasLocalFolderConflictWithSyncUseCaseTest {

    private lateinit var underTest: HasLocalFolderConflictWithSyncUseCase

    private val getLocalSyncOrBackupUriPathUseCase = mock<GetLocalSyncOrBackupUriPathUseCase>()
    private val getPathByDocumentContentUriUseCase = mock<GetPathByDocumentContentUriUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun setUp() {

        underTest = HasLocalFolderConflictWithSyncUseCase(
            getLocalSyncOrBackupUriPathUseCase = getLocalSyncOrBackupUriPathUseCase,
            getPathByDocumentContentUriUseCase = getPathByDocumentContentUriUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun cleanUp() {
        reset(
            getLocalSyncOrBackupUriPathUseCase,
            getPathByDocumentContentUriUseCase,
            getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that false is returned when feature flag is disabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)

        val result = underTest("/storage/emulated/0/DCIM")

        assertThat(result).isFalse()
    }

    @Test
    fun `test that false is returned when local folder path is empty`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)

        val result = underTest("")

        assertThat(result).isFalse()
    }

    @Test
    fun `test that true is returned when local folder overlaps with sync folder`() = runTest {
        val syncLocalUri = "content://com.android.externalstorage/tree/primary%3ASync"
        val syncLocalPath = "/storage/emulated/0/Sync"
        val selectedPath = "/storage/emulated/0/Sync"

        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getLocalSyncOrBackupUriPathUseCase()).thenReturn(
            listOf(
                createLocalFolderUriPath(localFolderPath = syncLocalUri)
            )
        )
        whenever(getPathByDocumentContentUriUseCase(syncLocalUri))
            .thenReturn(syncLocalPath)

        val result = underTest(selectedPath)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that false is returned when local folder does not overlap with sync folder`() =
        runTest {
            val syncLocalUri = "content://com.android.externalstorage/tree/primary%3ASync"
            val syncLocalPath = "/storage/emulated/0/Sync"
            val selectedPath = "/storage/emulated/0/DCIM"

            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getLocalSyncOrBackupUriPathUseCase()).thenReturn(
                listOf(
                    createLocalFolderUriPath(localFolderPath = syncLocalUri)
                )
            )
            whenever(getPathByDocumentContentUriUseCase(syncLocalUri))
                .thenReturn(syncLocalPath)

            val result = underTest(selectedPath)

            assertThat(result).isFalse()
        }

    @Test
    fun `test that false is returned when URI resolution returns null`() = runTest {
        val syncLocalUri = "content://com.android.externalstorage/tree/primary%3ASync"

        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getLocalSyncOrBackupUriPathUseCase()).thenReturn(
            listOf(
                createLocalFolderUriPath(localFolderPath = syncLocalUri)
            )
        )
        whenever(getPathByDocumentContentUriUseCase(syncLocalUri))
            .thenReturn(null)

        val result = underTest("/storage/emulated/0/DCIM")

        assertThat(result).isFalse()
    }

    private fun createLocalFolderUriPath(
        localFolderPath: String = "",
    ) = UriPath(localFolderPath)
}
