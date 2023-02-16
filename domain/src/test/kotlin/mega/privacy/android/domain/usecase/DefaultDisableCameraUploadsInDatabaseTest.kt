package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

/**
 * Test class for [DisableCameraUploadsInDatabase]
 */
@ExperimentalCoroutinesApi
class DefaultDisableCameraUploadsInDatabaseTest {
    private lateinit var underTest: DisableCameraUploadsInDatabase

    private val clearSyncRecords = mock<ClearSyncRecords>()
    private val disableCameraUploadSettings = mock<DisableCameraUploadSettings>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()

    @Before
    fun setUp() {
        underTest = DefaultDisableCameraUploadsInDatabase(
            clearSyncRecords = clearSyncRecords,
            disableCameraUploadSettings = disableCameraUploadSettings,
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
                    disableCameraUploadSettings,
                    resetCameraUploadTimeStamps
                )
            ) {
                verify(resetCameraUploadTimeStamps, times(1)).invoke(clearCamSyncRecords = true)
                verify(clearSyncRecords, times(1)).invoke()
                verify(disableCameraUploadSettings, times(1)).invoke()
            }
        }
}