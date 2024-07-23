package mega.privacy.android.feature.devicecenter.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.domain.usecase.GetDevicesUseCase
import mega.privacy.android.feature.devicecenter.ui.mapper.DeviceUINodeListMapper
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [DeviceCenterViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterViewModelTest {
    private lateinit var underTest: DeviceCenterViewModel

    private val accountDetailFlow = MutableStateFlow(AccountDetail())
    private val getDevicesUseCase = mock<GetDevicesUseCase>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val deviceUINodeListMapper = mock<DeviceUINodeListMapper>()

    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }

    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock {
        onBlocking { invoke() }.thenReturn(accountDetailFlow)
    }

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock {
        onBlocking { invoke(any()) } doReturn false
    }

    private val isCameraUploadsEnabled = true
    private val ownDeviceFolderUINode = NonBackupDeviceFolderUINode(
        id = "ABCD-EFGH",
        name = "Camera uploads",
        icon = FolderIconType.CameraUploads,
        status = DeviceCenterUINodeStatus.UpToDate,
        rootHandle = 789012L,
        localFolderPath = "storage/emulated/0/DCIM/Camera",
    )
    private val ownDeviceUINode = OwnDeviceUINode(
        id = "1234-5678",
        name = "Own Device",
        icon = DeviceIconType.Android,
        status = DeviceCenterUINodeStatus.UpToDate,
        folders = listOf(ownDeviceFolderUINode),
    )

    @BeforeEach
    fun resetMocks() {
        reset(
            getDevicesUseCase,
            isCameraUploadsEnabledUseCase,
            deviceUINodeListMapper,
        )

        runBlocking {
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_I
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flowOf(accountDetail)
            )
        }

        underTest = DeviceCenterViewModel(
            getDevicesUseCase = getDevicesUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            deviceUINodeListMapper = deviceUINodeListMapper,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }

    private suspend fun setupDefaultMocks() {
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(
            getDevicesUseCase()
        ).thenReturn(listOf(mock<OwnDeviceNode>()))
        whenever(
            deviceUINodeListMapper(
                deviceNodes = any(),
            )
        ).thenReturn(listOf(ownDeviceUINode))
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
                whenever(
                    deviceUINodeListMapper(
                        deviceNodes = any(),
                    )
                ).thenReturn(listOf(updatedOwnDeviceUINode))

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
                whenever(
                    deviceUINodeListMapper(
                        deviceNodes = any(),
                    )
                ).thenReturn(listOf(updatedOwnDeviceUINode))

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
                whenever(
                    deviceUINodeListMapper(
                        deviceNodes = any(),
                    )
                ).thenReturn(listOf(updatedOwnDeviceUINode))

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

    @Test
    fun `test that onSearchQueryChanged updates the search query and filters results`() = runTest {
        val query = "Galaxy S24"
        val deviceEntities: List<DeviceNode> = mock()
        val firstItem = mock<DeviceUINode> {
            on { it.id } doReturn "1234-5678"
            on { it.name } doReturn "Samsung Galaxy S24"
        }
        val secondItem = mock<DeviceUINode> {
            on { it.id } doReturn "9012-3456"
            on { it.name } doReturn "Samsung Galaxy S24 Ultra"
        }
        val thirdItem = mock<DeviceUINode> {
            on { it.id } doReturn "ABCD-EFGH"
            on { it.name } doReturn "Samsung Galaxy S22"
        }
        val itemsToDisplay = listOf(
            firstItem,
            secondItem,
            thirdItem
        )
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
        whenever(
            getDevicesUseCase()
        ).thenReturn(deviceEntities)
        whenever(
            deviceUINodeListMapper(
                deviceNodes = deviceEntities,
            )
        ).thenReturn(itemsToDisplay)

        underTest.getBackupInfo()
        underTest.onSearchQueryChanged(query)

        assertThat(underTest.state.value.searchQuery).isEqualTo(query)
        assertThat(underTest.state.value.filteredUiItems).isEqualTo(listOf(firstItem, secondItem))
    }

    @Test
    fun `test that on search close clicked resets the filtered items and collapses the search`() =
        runTest {
            val query = "Galaxy S24"
            val deviceEntities: List<DeviceNode> = mock()
            val firstItem = mock<DeviceUINode> {
                on { it.id } doReturn "1234-5678"
                on { it.name } doReturn "Samsung Galaxy S24"
            }
            val secondItem = mock<DeviceUINode> {
                on { it.id } doReturn "9012-3456"
                on { it.name } doReturn "Samsung Galaxy S24 Ultra"
            }
            val thirdItem = mock<DeviceUINode> {
                on { it.id } doReturn "ABCD-EFGH"
                on { it.name } doReturn "Samsung Galaxy S22"
            }
            val itemsToDisplay = listOf(
                firstItem,
                secondItem,
                thirdItem
            )
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                getDevicesUseCase()
            ).thenReturn(deviceEntities)
            whenever(
                deviceUINodeListMapper(
                    deviceNodes = deviceEntities,
                )
            ).thenReturn(itemsToDisplay)

            underTest.getBackupInfo()
            underTest.onSearchQueryChanged(query)
            underTest.onSearchCloseClicked()

            assertThat(underTest.state.value.searchWidgetState).isEqualTo(SearchWidgetState.COLLAPSED)
            assertThat(underTest.state.value.filteredUiItems).isNull()
        }

    @Test
    fun `test that onSearchClicked expands the searchWidgetState`() = runTest {
        underTest.onSearchClicked()

        assertThat(underTest.state.value.searchWidgetState).isEqualTo(SearchWidgetState.EXPANDED)
    }

    @Test
    fun `test that account type state is updated when monitorAccountDetail emits data`() =
        runTest {
            val expected = AccountDetail()
            accountDetailFlow.emit(expected)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isFreeAccount).isEqualTo(
                    expected.levelDetail?.accountType == AccountType.FREE
                )
            }
        }
}