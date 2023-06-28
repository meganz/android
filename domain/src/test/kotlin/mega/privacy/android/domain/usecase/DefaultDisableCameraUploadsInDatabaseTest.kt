package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsSettingsUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

/**
 * Test class for [DisableCameraUploadsInDatabase]
 */
@ExperimentalCoroutinesApi
class DefaultDisableCameraUploadsInDatabaseTest {
    private lateinit var underTest: DisableCameraUploadsInDatabase

    private val clearSyncRecords = mock<ClearSyncRecords>()
    private val disableCameraUploadsSettingsUseCase = mock<DisableCameraUploadsSettingsUseCase>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()

    @Before
    fun setUp() {
        underTest = DefaultDisableCameraUploadsInDatabase(
            clearSyncRecords = clearSyncRecords,
            disableCameraUploadsSettingsUseCase = disableCameraUploadsSettingsUseCase,
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
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
