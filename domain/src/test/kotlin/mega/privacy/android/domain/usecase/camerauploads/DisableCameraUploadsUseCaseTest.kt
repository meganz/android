package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset

/**
 * Test class for [DisableCameraUploadsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisableCameraUploadsUseCaseTest {
    private lateinit var underTest: DisableCameraUploadsUseCase

    private val clearSyncRecords = mock<ClearSyncRecords>()
    private val disableCameraUploadsSettingsUseCase = mock<DisableCameraUploadsSettingsUseCase>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()

    @BeforeAll
    fun setUp() {
        underTest = DisableCameraUploadsUseCase(
            clearSyncRecords = clearSyncRecords,
            disableCameraUploadsSettingsUseCase = disableCameraUploadsSettingsUseCase,
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            clearSyncRecords,
            disableCameraUploadsSettingsUseCase,
            resetCameraUploadTimeStamps,
        )
    }

    @Test
    fun `test that disabling camera uploads in the database are executed by use cases in a specific order`() =
        runTest {
            underTest()

            with(
                inOrder(
                    clearSyncRecords,
                    disableCameraUploadsSettingsUseCase,
                    resetCameraUploadTimeStamps
                )
            ) {
                verify(resetCameraUploadTimeStamps).invoke(clearCamSyncRecords = true)
                verify(clearSyncRecords).invoke()
                verify(disableCameraUploadsSettingsUseCase).invoke()
            }
        }
}
