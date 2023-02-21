package test.mega.privacy.android.app.presentation.settings.camerauploads

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExperimentalCoroutinesApi
class SettingsCameraUploadsViewModelTest {

    private lateinit var underTest: SettingsCameraUploadsViewModel

    private val checkEnableCameraUploadsStatus = mock<CheckEnableCameraUploadsStatus>()
    private val clearCacheDirectory = mock<ClearCacheDirectory>()
    private val disableCameraUploadsInDatabase = mock<DisableCameraUploadsInDatabase>()
    private val disableMediaUploadSettings = mock<DisableMediaUploadSettings>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()
    private val resetMediaUploadTimeStamps = mock<ResetMediaUploadTimeStamps>()
    private val restorePrimaryTimestamps = mock<RestorePrimaryTimestamps>()
    private val restoreSecondaryTimestamps = mock<RestoreSecondaryTimestamps>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = SettingsCameraUploadsViewModel(
            checkEnableCameraUploadsStatus = checkEnableCameraUploadsStatus,
            clearCacheDirectory = clearCacheDirectory,
            disableCameraUploadsInDatabase = disableCameraUploadsInDatabase,
            disableMediaUploadSettings = disableMediaUploadSettings,
            monitorConnectivity = mock(),
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            restorePrimaryTimestamps = restorePrimaryTimestamps,
            restoreSecondaryTimestamps = restoreSecondaryTimestamps,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val state = awaitItem()
            assertFalse(state.shouldShowBusinessAccountPrompt)
            assertFalse(state.shouldShowBusinessAccountSuspendedPrompt)
            assertFalse(state.shouldTriggerCameraUploads)
            assertFalse(state.shouldShowMediaPermissionsRationale)
            assertFalse(state.shouldShowNotificationPermissionRationale)
        }
    }

    /**
     * Mocks the value of [checkEnableCameraUploadsStatus] and calls the ViewModel method
     *
     * @param status The [EnableCameraUploadsStatus] to mock the Use Case
     */
    private suspend fun handleEnableCameraUploads(status: EnableCameraUploadsStatus) {
        whenever(checkEnableCameraUploadsStatus()).thenReturn(status)
        underTest.handleEnableCameraUploads()
    }

    @Test
    fun `test that shouldShowBusinessAccountPrompt is true when checkEnableCameraUploadsStatus returns SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT)

            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.shouldShowBusinessAccountPrompt)
            }
        }

    @Test
    fun `test that shouldShowBusinessAccountSuspendedPrompt is true when checkEnableCameraUploadsStatus returns SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT)

            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.shouldShowBusinessAccountSuspendedPrompt)
            }
        }

    @Test
    fun `test that shouldTriggerCameraUploads is true when checkEnableCameraUploadsStatus returns CAN_ENABLE_CAMERA_UPLOADS`() =
        runTest {
            handleEnableCameraUploads(status = EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)

            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.shouldTriggerCameraUploads)
            }
        }

    @Test
    fun `test that shouldShowBusinessAccountPrompt is false when calling resetBusinessAccountPromptState`() =
        runTest {
            underTest.resetBusinessAccountPromptState()

            underTest.state.test {
                val state = awaitItem()
                assertFalse(state.shouldShowBusinessAccountPrompt)
            }
        }

    @Test
    fun `test that shouldShowBusinessAccountSuspendedPrompt is false when calling resetBusinessAccountSuspendedPromptState`() =
        runTest {
            underTest.resetBusinessAccountSuspendedPromptState()

            underTest.state.test {
                val state = awaitItem()
                assertFalse(state.shouldShowBusinessAccountSuspendedPrompt)
            }
        }

    /**
     * Performs an assertion test of shouldTriggerCameraUploads
     * @param assertValue Boolean value assigned to shouldTriggerCameraUploads for assertion
     */
    private fun assertShouldTriggerCameraUploads(assertValue: Boolean) = runTest {
        underTest.setTriggerCameraUploadsState(assertValue)

        underTest.state.test {
            val state = awaitItem()
            assertEquals(state.shouldTriggerCameraUploads, assertValue)
        }
    }

    @Test
    fun `test that shouldTriggerCameraUploads is true when calling setTriggerCameraUploads with true value`() =
        assertShouldTriggerCameraUploads(true)

    @Test
    fun `test that shouldTriggerCameraUploads is false when calling setTriggerCameraUploads with false value`() =
        assertShouldTriggerCameraUploads(false)

    @Test
    fun `test that shouldShowMediaPermissionsRationale is false when set to false`() = runTest {
        underTest.setMediaPermissionsRationaleState(false)

        underTest.state.test {
            val state = awaitItem()
            assertFalse(state.shouldShowMediaPermissionsRationale)
        }
    }

    @Test
    fun `test that shouldShowMediaPermissionsRationale is true when set to true`() = runTest {
        underTest.setMediaPermissionsRationaleState(true)

        underTest.state.test {
            val state = awaitItem()
            assertTrue(state.shouldShowMediaPermissionsRationale)
        }
    }

    @Test
    fun `test that shouldShowNotificationPermissionRationale is true when set to true`() =
        runTest {
            underTest.setNotificationPermissionRationaleState(true)

            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.shouldShowNotificationPermissionRationale)
            }
        }

    @Test
    fun `test that shouldShowNotificationPermissionRationale is false when set to false`() =
        runTest {
            underTest.setNotificationPermissionRationaleState(false)

            underTest.state.test {
                val state = awaitItem()
                assertFalse(state.shouldShowNotificationPermissionRationale)
            }
        }

    @Test
    fun `test that restorePrimaryTimestamps is invoked when calling restorePrimaryTimestampsAndSyncRecordProcess`() =
        runTest {
            underTest.restorePrimaryTimestampsAndSyncRecordProcess()

            verify(restorePrimaryTimestamps, times(1)).invoke()
        }

    @Test
    fun `test that restoreSecondaryTimestamps is invoked when calling restoreSecondaryTimestampsAndSyncRecordProcess`() =
        runTest {
            underTest.restoreSecondaryTimestampsAndSyncRecordProcess()

            verify(restoreSecondaryTimestamps, times(1)).invoke()
        }

    @Test
    fun `test that resetCameraUploadTimeStamps and clearCacheDirectory are invoked in order when calling resetTimestampsAndCacheDirectory`() =
        runTest {
            underTest.resetTimestampsAndCacheDirectory()

            with(inOrder(clearCacheDirectory, resetCameraUploadTimeStamps)) {
                verify(resetCameraUploadTimeStamps, times(1)).invoke(clearCamSyncRecords = true)
                verify(clearCacheDirectory, times(1)).invoke()
            }
        }

    @Test
    fun `test that disableCameraUploadsInDatabase is invoked when calling disableCameraUploadsInDB`() =
        runTest {
            underTest.disableCameraUploadsInDB()

            verify(disableCameraUploadsInDatabase, times(1)).invoke()
        }

    @Test
    fun `test that media uploads are disabled when calling disableMediaUploads`() = runTest {
        underTest.disableMediaUploads()

        verify(resetMediaUploadTimeStamps, times(1)).invoke()
        verify(disableMediaUploadSettings, times(1)).invoke()
    }
}