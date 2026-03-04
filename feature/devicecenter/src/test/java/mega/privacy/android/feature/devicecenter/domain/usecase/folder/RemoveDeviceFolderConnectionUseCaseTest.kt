package mega.privacy.android.feature.devicecenter.domain.usecase.folder

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.backup.RemoveDeviceFolderConnectionUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [RemoveDeviceFolderConnectionUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveDeviceFolderConnectionUseCaseTest {

    private lateinit var underTest: RemoveDeviceFolderConnectionUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RemoveDeviceFolderConnectionUseCaseImpl(
            cameraUploadsRepository = cameraUploadsRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that when invoked with correct backupId and returns BackupRemovalStatus`() =
        runTest {
            val backupId = 123L
            val expectedStatus = BackupRemovalStatus(
                backupId = backupId,
                isOutdated = false
            )
            whenever(cameraUploadsRepository.removeBackupFolder(backupId)).thenReturn(expectedStatus)
            val actual = underTest(backupId)

            assertThat(actual).isEqualTo(expectedStatus)
        }

    @Test
    fun `test that when invoked it returns BackupRemovalStatus with outdated flag true when backup is outdated`() =
        runTest {
            // Given
            val backupId = 456L
            val expectedStatus = BackupRemovalStatus(
                backupId = backupId,
                isOutdated = true
            )
            whenever(cameraUploadsRepository.removeBackupFolder(backupId)).thenReturn(expectedStatus)

            val actual = underTest(backupId)

            assertThat(actual).isEqualTo(expectedStatus)
        }
}
