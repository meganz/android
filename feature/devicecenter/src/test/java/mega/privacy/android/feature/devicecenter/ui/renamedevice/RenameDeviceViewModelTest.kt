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
import mega.privacy.android.feature.devicecenter.R
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset

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
            assertThat(initialState.errorMessage).isNull()
            assertThat(initialState.renameSuccessfulEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that the error message is cleared`() = runTest {
        underTest.clearErrorMessage()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `test that renaming the device is successful`() = runTest {
        val deviceId = "12345-6789"
        val newDeviceName = "New Device Name"
        val existingDeviceNames = listOf("Old Device Name")

        underTest.renameDevice(
            deviceId = deviceId,
            newDeviceName = newDeviceName,
            existingDeviceNames = existingDeviceNames,
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
            assertThat(state.renameSuccessfulEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that renaming the device fails when the new device name is empty`() =
        runTest {
            val deviceId = "12345-6789"
            val newDeviceName = ""
            val existingDeviceNames = listOf("Old Device Name")

            underTest.renameDevice(
                deviceId = deviceId,
                newDeviceName = newDeviceName,
                existingDeviceNames = existingDeviceNames,
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isEqualTo(R.string.device_center_rename_device_dialog_error_message_empty_device_name)
            }
        }

    @Test
    fun `test that renaming the device fails when the new device name already exists in the current list of devices`() =
        runTest {
            val deviceId = "12345-6789"
            val newDeviceName = "Old Device Name"
            val existingDeviceNames = listOf(newDeviceName)

            underTest.renameDevice(
                deviceId = deviceId,
                newDeviceName = newDeviceName,
                existingDeviceNames = existingDeviceNames,
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isEqualTo(R.string.device_center_rename_device_dialog_error_message_name_already_exists)
            }
        }

    @ParameterizedTest(name = "new device name: {0}")
    @ValueSource(
        strings = [
            "Samsung\"", "Samsung*", "Samsung/", "Samsung:", "Samsung<", "Samsung>", "Samsung?",
            "Samsung\\", "Samsung|",
        ]
    )
    fun `test that renaming the device fails when the new device name contains invalid characters`(
        deviceName: String,
    ) = runTest {
        val deviceId = "12345-6789"
        val existingDeviceNames = listOf("Old Device Name")

        underTest.renameDevice(
            deviceId = deviceId,
            newDeviceName = deviceName,
            existingDeviceNames = existingDeviceNames,
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isEqualTo(R.string.device_center_rename_device_dialog_error_message_invalid_characters)
        }
    }

    @Test
    fun `test that the rename successful event has been consumed`() = runTest {
        underTest.resetRenameSuccessfulEvent()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.renameSuccessfulEvent).isEqualTo(consumed)
        }
    }
}