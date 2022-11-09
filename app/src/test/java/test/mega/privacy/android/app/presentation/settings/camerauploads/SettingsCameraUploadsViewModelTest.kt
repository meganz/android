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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
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

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = SettingsCameraUploadsViewModel(
            checkEnableCameraUploadsStatus = checkEnableCameraUploadsStatus,
            restorePrimaryTimestamps = mock(),
            restoreSecondaryTimestamps = mock(),
            monitorConnectivity = mock(),
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
}
