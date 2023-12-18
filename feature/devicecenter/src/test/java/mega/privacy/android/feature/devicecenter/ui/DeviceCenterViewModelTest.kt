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
        rootHandle = 789012L,
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
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getDevicesUseCase,
            isCameraUploadsEnabledUseCase,
            deviceUINodeListMapper,
        )
        underTest = DeviceCenterViewModel(
            getDevicesUseCase = getDevicesUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            deviceUINodeListMapper = deviceUINodeListMapper,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun setupDefaultMocks() {
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(getDevicesUseCase(any())).thenReturn(listOf(mock<OwnDeviceNode>()))
        whenever(deviceUINodeListMapper(any())).thenReturn(listOf(ownDeviceUINode))
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.devices).isEmpty()
            assertThat(initialState.isCameraUploadsEnabled).isFalse()
            assertThat(initialState.isInitialLoadingFinished).isFalse()
            assertThat(initialState.selectedDevice).isNull()
            assertThat(initialState.menuClickedDevice).isNull()
            assertThat(initialState.deviceToRename).isNull()
            assertThat(initialState.itemsToDisplay).isEmpty()
            assertThat(initialState.exitFeature).isEqualTo(consumed)
            assertThat(initialState.renameDeviceSuccess).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that the backup information is retrieved`() = runTest {
        setupDefaultMocks()
        underTest.getBackupInfo()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.devices).isEqualTo(listOf(ownDeviceUINode))
            assertThat(state.isCameraUploadsEnabled).isEqualTo(isCameraUploadsEnabled)
            assertThat(state.itemsToDisplay).isEqualTo(listOf(ownDeviceUINode))
        }
    }

    @Test
    fun `test that the initial loading is completed when the backup information is retrieved`() =
        runTest {
            setupDefaultMocks()
            underTest.getBackupInfo()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isInitialLoadingFinished).isTrue()
            }
        }

    @Test
    fun `test that the initial loading is still completed when retrieving the backup information throws an exception`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenThrow(RuntimeException())
            underTest.getBackupInfo()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isInitialLoadingFinished).isTrue()
            }
        }

    @Test
    fun `test that the selected device is updated when the backup information is refreshed`() =
        runTest {
            setupDefaultMocks()
            underTest.getBackupInfo()
            underTest.state.test {
                val firstState = awaitItem()
                assertThat(firstState.devices).isEqualTo(listOf(ownDeviceUINode))
                assertThat(firstState.selectedDevice).isNull()
                assertThat(firstState.itemsToDisplay).isEqualTo(listOf(ownDeviceUINode))

                underTest.showDeviceFolders(ownDeviceUINode)
                val secondState = awaitItem()
                assertThat(secondState.selectedDevice).isEqualTo(ownDeviceUINode)
                assertThat(secondState.itemsToDisplay).isEqualTo(listOf(ownDeviceFolderUINode))

                val updatedOwnDeviceFolderUINode = ownDeviceFolderUINode.copy(
                    status = DeviceCenterUINodeStatus.Initializing,
                )
                val updatedOwnDeviceUINode = ownDeviceUINode.copy(
                    status = DeviceCenterUINodeStatus.Initializing,
                    folders = listOf(updatedOwnDeviceFolderUINode)
                )
                whenever(deviceUINodeListMapper(any())).thenReturn(listOf(updatedOwnDeviceUINode))

                underTest.getBackupInfo()
                val thirdState = awaitItem()
                assertThat(thirdState.devices).isEqualTo(listOf(updatedOwnDeviceUINode))
                assertThat(thirdState.selectedDevice).isEqualTo(updatedOwnDeviceUINode)
                assertThat(thirdState.itemsToDisplay).isEqualTo(listOf(updatedOwnDeviceFolderUINode))
            }
        }

    @Test
    fun `test that the selected device is null when it is missing from the refreshed backup information`() =
        runTest {
            setupDefaultMocks()
            underTest.getBackupInfo()
            underTest.state.test {
                val firstState = awaitItem()
                assertThat(firstState.devices).isEqualTo(listOf(ownDeviceUINode))
                assertThat(firstState.selectedDevice).isNull()
                assertThat(firstState.itemsToDisplay).isEqualTo(listOf(ownDeviceUINode))

                underTest.showDeviceFolders(ownDeviceUINode)
                val secondState = awaitItem()
                assertThat(secondState.selectedDevice).isEqualTo(ownDeviceUINode)
                assertThat(secondState.itemsToDisplay).isEqualTo(listOf(ownDeviceFolderUINode))

                val updatedOwnDeviceUINode = ownDeviceUINode.copy(
                    id = "9012-3456",
                    status = DeviceCenterUINodeStatus.Initializing,
                )
                whenever(deviceUINodeListMapper(any())).thenReturn(listOf(updatedOwnDeviceUINode))

                underTest.getBackupInfo()
                val thirdState = awaitItem()
                assertThat(thirdState.devices).isEqualTo(listOf(updatedOwnDeviceUINode))
                assertThat(thirdState.selectedDevice).isNull()
                assertThat(thirdState.itemsToDisplay).isEqualTo(listOf(updatedOwnDeviceUINode))
            }
        }

    @Test
    fun `test that the selected device is still null when there is no selected device and the backup information is refreshed`() =
        runTest {
            setupDefaultMocks()
            underTest.getBackupInfo()
            underTest.state.test {
                val firstState = awaitItem()
                assertThat(firstState.devices).isEqualTo(listOf(ownDeviceUINode))
                assertThat(firstState.selectedDevice).isNull()
                assertThat(firstState.itemsToDisplay).isEqualTo(listOf(ownDeviceUINode))

                val updatedOwnDeviceUINode = ownDeviceUINode.copy(
                    status = DeviceCenterUINodeStatus.Initializing,
                )
                whenever(deviceUINodeListMapper(any())).thenReturn(listOf(updatedOwnDeviceUINode))

                underTest.getBackupInfo()
                val secondState = awaitItem()
                assertThat(secondState.devices).isEqualTo(listOf(updatedOwnDeviceUINode))
                assertThat(secondState.selectedDevice).isNull()
                assertThat(secondState.itemsToDisplay).isEqualTo(listOf(updatedOwnDeviceUINode))
            }
        }

    @Test
    fun `test that the list of folders are shown when a device is selected`() = runTest {
        setupDefaultMocks()
        underTest.getBackupInfo()
        underTest.showDeviceFolders(ownDeviceUINode)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.devices).isEqualTo(listOf(ownDeviceUINode))
            assertThat(state.selectedDevice).isEqualTo(ownDeviceUINode)
            assertThat(state.itemsToDisplay).isEqualTo(listOf(ownDeviceFolderUINode))
        }
    }

    @Test
    fun `test that the bottom dialog is shown when the device context menu is selected`() =
        runTest {
            setupDefaultMocks()
            underTest.setMenuClickedDevice(ownDeviceUINode)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.menuClickedDevice).isEqualTo(ownDeviceUINode)
            }
        }

    @Test
    fun `test that the bottom dialog is hidden when a back press event occurs`() = runTest {
        setupDefaultMocks()
        underTest.setMenuClickedDevice(ownDeviceUINode)
        underTest.handleBackPress()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.menuClickedDevice).isNull()
        }
    }

    @Test
    fun `test that the rename device dialog is shown`() =
        runTest {
            setupDefaultMocks()
            underTest.setDeviceToRename(ownDeviceUINode)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.deviceToRename).isEqualTo(ownDeviceUINode)
            }
        }

    @Test
    fun `test that the rename device dialog is hidden when dismissed by the user`() = runTest {
        setupDefaultMocks()
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
            setupDefaultMocks()
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
            setupDefaultMocks()
            underTest.getBackupInfo()
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
            setupDefaultMocks()
            underTest.handleBackPress()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.exitFeature).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that the user has successfully renamed a device`() = runTest {
        underTest.handleRenameDeviceSuccess()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deviceToRename).isNull()
            assertThat(state.renameDeviceSuccess).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that the rename device success event is consumed`() = runTest {
        underTest.resetRenameDeviceSuccessEvent()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.renameDeviceSuccess).isEqualTo(consumed)
        }
    }
}