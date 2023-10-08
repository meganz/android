package mega.privacy.android.feature.devicecenter.ui

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
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.domain.usecase.GetDevicesUseCase
import mega.privacy.android.feature.devicecenter.ui.mapper.DeviceUINodeListMapper
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
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
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val deviceUINodeListMapper = mock<DeviceUINodeListMapper>()

    private val isCameraUploadsEnabled = true
    private val ownDeviceFolderUINode = NonBackupDeviceFolderUINode(
        id = "ABCD-EFGH",
        name = "Camera uploads",
        icon = FolderIconType.CameraUploads,
        status = DeviceCenterUINodeStatus.UpToDate,
    )
    private val ownDeviceUINode = OwnDeviceUINode(
        id = "1234-5678",
        name = "Own Device",
        icon = DeviceIconType.Android,
        status = DeviceCenterUINodeStatus.UpToDate,
        folders = listOf(ownDeviceFolderUINode),
    )

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = DeviceCenterViewModel(
            getDevicesUseCase = getDevicesUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            deviceUINodeListMapper = deviceUINodeListMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getDevicesUseCase, isCameraUploadsEnabledUseCase, deviceUINodeListMapper)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setUnderTest() {
        underTest = DeviceCenterViewModel(
            getDevicesUseCase = getDevicesUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            deviceUINodeListMapper = deviceUINodeListMapper,
        )
    }

    private suspend fun setupMocks() {
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(getDevicesUseCase(any())).thenReturn(listOf(mock<OwnDeviceNode>()))
        whenever(deviceUINodeListMapper(any())).thenReturn(listOf(ownDeviceUINode))
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        setUnderTest()

        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.devices).isEmpty()
            assertThat(initialState.isCameraUploadsEnabled).isFalse()
            assertThat(initialState.isInitialLoadingOngoing).isFalse()
            assertThat(initialState.selectedDevice).isNull()
            assertThat(initialState.menuIconClickedNode).isNull()
            assertThat(initialState.deviceToRename).isNull()
            assertThat(initialState.itemsToDisplay).isEmpty()
            assertThat(initialState.exitFeature).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that the backup information is shown when accessing the device center`() = runTest {
        setupMocks()
        setUnderTest()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.devices).isEqualTo(listOf(ownDeviceUINode))
            assertThat(state.isCameraUploadsEnabled).isEqualTo(isCameraUploadsEnabled)
            assertThat(state.itemsToDisplay).isEqualTo(listOf(ownDeviceUINode))
        }
    }

    @Test
    fun `test that the list of folders are shown when a device is selected`() = runTest {
        setupMocks()
        setUnderTest()
        underTest.showDeviceFolders(ownDeviceUINode)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.devices).isEqualTo(listOf(ownDeviceUINode))
            assertThat(state.selectedDevice).isEqualTo(ownDeviceUINode)
            assertThat(state.itemsToDisplay).isEqualTo(listOf(ownDeviceFolderUINode))
        }
    }

    @Test
    fun `test that the bottom dialog is shown when the menu icon of a node is selected`() =
        runTest {
            setupMocks()
            setUnderTest()
            underTest.setMenuClickedNode(ownDeviceUINode)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.menuIconClickedNode).isEqualTo(ownDeviceUINode)
            }
        }

    @Test
    fun `test that the bottom dialog is hidden when a back press event occurs`() = runTest {
        setupMocks()
        setUnderTest()
        underTest.setMenuClickedNode(ownDeviceUINode)
        underTest.handleBackPress()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.menuIconClickedNode).isNull()
        }
    }

    @Test
    fun `test that the rename device dialog is shown`() =
        runTest {
            setupMocks()
            setUnderTest()
            underTest.setDeviceToRename(ownDeviceUINode)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.deviceToRename).isEqualTo(ownDeviceUINode)
            }
        }

    @Test
    fun `test that the rename device dialog is hidden when dismissed by the user`() = runTest {
        setupMocks()
        setUnderTest()
        underTest.setDeviceToRename(ownDeviceUINode)
        underTest.resetDeviceToRename()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deviceToRename).isNull()
        }
    }

    @Test
    fun `test that the rename device dialog is hidden when a back press event occurs`() =
        runTest {
            setupMocks()
            setUnderTest()
            underTest.setDeviceToRename(ownDeviceUINode)
            underTest.handleBackPress()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.deviceToRename).isNull()
            }
        }

    @Test
    fun `test that the user goes back to device view when going back from folder view`() =
        runTest {
            setupMocks()
            setUnderTest()
            underTest.showDeviceFolders(ownDeviceUINode)
            underTest.handleBackPress()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.devices).isEqualTo(listOf(ownDeviceUINode))
                assertThat(state.selectedDevice).isNull()
                assertThat(state.itemsToDisplay).isEqualTo(listOf(ownDeviceUINode))
            }
        }

    @Test
    fun `test that the user exits the device center when going back from device view`() =
        runTest {
            setupMocks()
            setUnderTest()
            underTest.handleBackPress()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.exitFeature).isEqualTo(triggered)
            }
        }
}