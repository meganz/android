package mega.privacy.android.feature.devicecenter.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.domain.usecase.GetDevicesUseCase
import mega.privacy.android.feature.devicecenter.ui.mapper.DeviceUINodeListMapper
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [DeviceCenterViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterViewModelTest {
    private lateinit var underTest: DeviceCenterViewModel

    private val getDevicesUseCase = mock<GetDevicesUseCase>()
    private val deviceUINodeListMapper = mock<DeviceUINodeListMapper>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = DeviceCenterViewModel(
            getDevicesUseCase = getDevicesUseCase,
            deviceUINodeListMapper = deviceUINodeListMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getDevicesUseCase, deviceUINodeListMapper)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setUnderTest() {
        underTest = DeviceCenterViewModel(
            getDevicesUseCase = getDevicesUseCase,
            deviceUINodeListMapper = deviceUINodeListMapper,
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        setUnderTest()

        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.nodes).isEmpty()
        }
    }

    @Test
    fun `test that the backup information is retrieved`() = runTest {
        val deviceId = "12345-6789"
        val deviceName = "MacBook Pro M2"

        val backupDevices = listOf(
            OwnDeviceNode(
                id = deviceId,
                name = deviceName,
                status = DeviceCenterNodeStatus.UpToDate,
                folders = listOf(mock()),
            ),
        )
        val expectedBackupUIDevices = listOf(
            OwnDeviceUINode(
                id = deviceId,
                name = deviceName,
                icon = DeviceIconType.PC,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = listOf(mock()),
            ),
        )

        whenever(getDevicesUseCase()).thenReturn(backupDevices)
        whenever(deviceUINodeListMapper(backupDevices)).thenReturn(expectedBackupUIDevices)

        setUnderTest()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodes).isEqualTo(expectedBackupUIDevices)
        }
    }
}