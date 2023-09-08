package mega.privacy.android.feature.devicecenter.ui.renamedevice

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.usecase.backup.RenameDeviceUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test Class for [RenameDeviceViewModel]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
internal class RenameDeviceViewModelTest {

    private lateinit var underTest: RenameDeviceViewModel

    private val renameDeviceUseCase = mock<RenameDeviceUseCase>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = RenameDeviceViewModel(renameDeviceUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(renameDeviceUseCase)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.renameSuccessfulEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that rename device is called`() = runTest {
        val deviceId = "12345-6789"
        val deviceName = "New Device Name"

        underTest.renameDevice(
            deviceId = deviceId,
            deviceName = deviceName,
        )
        verify(renameDeviceUseCase).invoke(
            deviceId = deviceId,
            deviceName = deviceName,
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.renameSuccessfulEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that rename successful event has been consumed`() = runTest {
        underTest.onResetRenameSuccessfulEvent()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.renameSuccessfulEvent).isEqualTo(consumed)
        }
    }
}