package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetUploadVideoSyncStatusUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetUploadVideoSyncStatusUseCaseTest {

    private lateinit var underTest: SetUploadVideoSyncStatusUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetUploadVideoSyncStatusUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "test that the sync status {0} is set ")
    @EnumSource(SyncStatus::class)
    fun `test that the upload video sync status is updated`(syncStatus: SyncStatus) = runTest {
        underTest(syncStatus)

        verify(cameraUploadRepository).setUploadVideoSyncStatus(syncStatus)
    }
}