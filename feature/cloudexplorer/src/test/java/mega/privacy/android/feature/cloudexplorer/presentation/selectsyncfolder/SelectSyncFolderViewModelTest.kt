package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerHandler
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerNodesResult
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SelectSyncFolderViewModelTest {

    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val syncFolderPickerHandler: SyncFolderPickerHandler = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()

    private lateinit var underTest: SelectSyncFolderViewModel

    private val rootFolderId = NodeId(123456L)

    @BeforeEach
    fun setUp() {
        Analytics.initialise(mock<AnalyticsTracker>())
    }

    @AfterEach
    fun resetAndTearDown() {
        Analytics.initialise(null)
        reset(
            getRootNodeIdUseCase,
            getFeatureFlagValueUseCase,
            syncFolderPickerHandler,
            syncPermissionsManager,
        )
    }

    private suspend fun initViewModel(
        folderHandle: Long = SelectSyncFolderViewModel.INVALID_FOLDER_HANDLE,
        isStopBackup: Boolean = false,
        stopBackupFolderName: String? = null,
        batteryOptimizationGranted: Boolean = true,
        nodesResult: SyncFolderPickerNodesResult = SyncFolderPickerNodesResult(
            restrictedNodes = emptyMap(),
            isSelectEnabled = true,
        ),
    ) {
        whenever(getRootNodeIdUseCase()).thenReturn(rootFolderId)
        whenever(syncPermissionsManager.isDisableBatteryOptimizationGranted())
            .thenReturn(batteryOptimizationGranted)
        whenever(
            syncFolderPickerHandler.monitorPickerNodes(any(), any(), anyOrNull())
        ).thenReturn(flowOf(nodesResult))
        underTest = SelectSyncFolderViewModel(
            args = SelectSyncFolderViewModel.Args(
                folderHandle = folderHandle,
                isStopBackup = isStopBackup,
                stopBackupFolderName = stopBackupFolderName,
            ),
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            syncFolderPickerHandler = syncFolderPickerHandler,
            syncPermissionsManager = syncPermissionsManager,
        )
    }

    private fun selectCurrentFolder() =
        underTest.handleAction(SelectSyncFolderAction.CurrentFolderSelected)

    /**
     * uiState is lazy and exposed with WhileSubscribed, so it only resolves the folder and applies
     * action-driven updates while it is being collected. This helper collects it, runs [action]
     * once the folder is resolved, and asserts on the resulting state.
     */
    private suspend fun TestScope.assertUiState(
        action: () -> Unit = {},
        assertions: (SelectSyncFolderUiState) -> Unit = {},
    ) {
        underTest.uiState.test {
            advanceUntilIdle()
            action()
            advanceUntilIdle()
            assertions(expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun SelectSyncFolderUiState.data() = this as SelectSyncFolderUiState.Data

    @Test
    fun `test that the root folder and its restrictions are resolved upon initialization`() =
        runTest {
            val restrictedNode = SyncFolderPickerRestrictedNode(
                nodeId = NodeId(987L),
                name = "Synced folder",
                isUsedBySyncOrBackup = true,
            )
            initViewModel(
                nodesResult = SyncFolderPickerNodesResult(
                    restrictedNodes = mapOf(restrictedNode.nodeId to restrictedNode),
                    isSelectEnabled = false,
                )
            )

            assertUiState { state ->
                val data = state.data()
                assertThat(data.currentFolderId).isEqualTo(rootFolderId)
                assertThat(data.restrictedNodes)
                    .containsExactly(restrictedNode.nodeId, restrictedNode)
                assertThat(data.isSelectEnabled).isFalse()
            }
            verify(syncFolderPickerHandler).monitorPickerNodes(
                folderId = rootFolderId,
                isStopBackup = false,
                stopBackupFolderName = null,
            )
        }

    @Test
    fun `test that an explicit folder handle is used without resolving the root folder`() =
        runTest {
            val folderId = NodeId(777L)
            initViewModel(folderHandle = folderId.longValue)

            assertUiState { state ->
                assertThat(state.data().currentFolderId).isEqualTo(folderId)
                assertThat(state.data().isSelectEnabled).isTrue()
            }
            verifyNoInteractions(getRootNodeIdUseCase)
            verify(syncFolderPickerHandler).monitorPickerNodes(
                folderId = folderId,
                isStopBackup = false,
                stopBackupFolderName = null,
            )
        }

    @Test
    fun `test that a conflict message is shown when the folder is used by a sync or backup`() =
        runTest {
            val conflictMessage = "folder conflict"
            initViewModel()
            whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
                .thenReturn(conflictMessage)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                assertThat(state.data().warningEvent)
                    .isEqualTo(triggered(LocalizedText.Literal(conflictMessage)))
            }
            verify(syncFolderPickerHandler, never()).validateNodeSyncability(any())
            verify(syncFolderPickerHandler, never()).saveSelectedFolder(any())
        }

    @Test
    fun `test that a syncability error is shown when the folder cannot be synced`() = runTest {
        val errorMessageRes = 12345
        initViewModel()
        whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
            .thenReturn(null)
        whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId))
            .thenReturn(errorMessageRes)

        assertUiState(action = { selectCurrentFolder() }) { state ->
            assertThat(state.data().warningEvent)
                .isEqualTo(triggered(LocalizedText.StringRes(errorMessageRes)))
        }
        verify(syncFolderPickerHandler, never()).saveSelectedFolder(any())
    }

    @Test
    fun `test that the folder is saved and confirmed when all permissions are granted`() =
        runTest {
            initViewModel()
            whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.saveSelectedFolder(rootFolderId)).thenReturn(true)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                assertThat(state.data().folderConfirmedEvent).isEqualTo(triggered)
            }
            verify(syncFolderPickerHandler).saveSelectedFolder(rootFolderId)
        }

    @Test
    fun `test that an error is shown when the selected folder cannot be saved`() = runTest {
        initViewModel()
        whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
            .thenReturn(null)
        whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId)).thenReturn(null)
        whenever(syncFolderPickerHandler.saveSelectedFolder(rootFolderId)).thenReturn(false)

        assertUiState(action = { selectCurrentFolder() }) { state ->
            val data = state.data()
            assertThat(data.folderConfirmedEvent).isEqualTo(consumed)
            assertThat(data.warningEvent)
                .isEqualTo(triggered(LocalizedText.StringRes(sharedR.string.general_text_error)))
        }
    }

    @Test
    fun `test that the battery optimization dialog is shown when the permission is not granted and the feature flag is enabled`() =
        runTest {
            initViewModel(batteryOptimizationGranted = false)
            whenever(getFeatureFlagValueUseCase(SyncFeatures.DisableBatteryOptimization))
                .thenReturn(true)
            whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId))
                .thenReturn(null)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                assertThat(state.data().disableBatteryOptimizationsEvent).isEqualTo(triggered)
            }
            verify(syncFolderPickerHandler, never()).saveSelectedFolder(any())
        }

    @Test
    fun `test that the battery optimization dialog is skipped when the feature flag is disabled`() =
        runTest {
            initViewModel(batteryOptimizationGranted = false)
            whenever(getFeatureFlagValueUseCase(SyncFeatures.DisableBatteryOptimization))
                .thenReturn(false)
            whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.saveSelectedFolder(rootFolderId)).thenReturn(true)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                val data = state.data()
                assertThat(data.disableBatteryOptimizationsEvent).isEqualTo(consumed)
                assertThat(data.folderConfirmedEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that a message is shown in stop backup mode when a folder with the same name exists`() =
        runTest {
            val folderName = "Backup folder"
            initViewModel(isStopBackup = true, stopBackupFolderName = folderName)
            whenever(syncFolderPickerHandler.folderNameExists(rootFolderId, folderName))
                .thenReturn(true)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                val data = state.data()
                assertThat(data.folderConfirmedEvent).isEqualTo(consumed)
                assertThat(data.warningEvent).isEqualTo(
                    triggered(
                        LocalizedText.StringRes(
                            sharedR.string.create_new_folder_dialog_error_existing_folder
                        )
                    )
                )
            }
            verify(syncFolderPickerHandler, never()).getFolderUsageConflictMessage(any())
            verify(syncFolderPickerHandler, never()).validateNodeSyncability(any())
            verify(syncFolderPickerHandler, never()).saveSelectedFolder(any())
        }

    @Test
    fun `test that selection proceeds in stop backup mode when no folder with the same name exists`() =
        runTest {
            val folderName = "Backup folder"
            initViewModel(isStopBackup = true, stopBackupFolderName = folderName)
            whenever(syncFolderPickerHandler.folderNameExists(rootFolderId, folderName))
                .thenReturn(false)
            whenever(syncFolderPickerHandler.saveSelectedFolder(rootFolderId)).thenReturn(true)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                assertThat(state.data().folderConfirmedEvent).isEqualTo(triggered)
            }
            verify(syncFolderPickerHandler, never()).getFolderUsageConflictMessage(any())
            verify(syncFolderPickerHandler, never()).validateNodeSyncability(any())
        }

    @Test
    fun `test that the remove connection node is set when a restricted folder with a backup id is clicked`() =
        runTest {
            val restrictedNode = SyncFolderPickerRestrictedNode(
                nodeId = NodeId(987L),
                name = "Other device backup",
                isUsedBySyncOrBackup = true,
                backupId = 555L,
                deviceName = "Other device",
            )
            initViewModel(
                nodesResult = SyncFolderPickerNodesResult(
                    restrictedNodes = mapOf(restrictedNode.nodeId to restrictedNode),
                    isSelectEnabled = false,
                )
            )

            assertUiState(
                action = {
                    underTest.handleAction(
                        SelectSyncFolderAction.RestrictedFolderClicked(restrictedNode.nodeId)
                    )
                }
            ) { state ->
                assertThat(state.data().removeConnectionNode).isEqualTo(restrictedNode)
            }
        }

    @Test
    fun `test that the remove connection node is not set when a restricted folder without a backup id is clicked`() =
        runTest {
            val restrictedNode = SyncFolderPickerRestrictedNode(
                nodeId = NodeId(987L),
                name = "Synced folder",
                isUsedBySyncOrBackup = true,
            )
            initViewModel(
                nodesResult = SyncFolderPickerNodesResult(
                    restrictedNodes = mapOf(restrictedNode.nodeId to restrictedNode),
                    isSelectEnabled = false,
                )
            )

            assertUiState(
                action = {
                    underTest.handleAction(
                        SelectSyncFolderAction.RestrictedFolderClicked(restrictedNode.nodeId)
                    )
                }
            ) { state ->
                assertThat(state.data().removeConnectionNode).isNull()
            }
        }

    @Test
    fun `test that confirming the connection removal removes it and shows a confirmation message`() =
        runTest {
            val backupId = 555L
            val restrictedNode = SyncFolderPickerRestrictedNode(
                nodeId = NodeId(987L),
                name = "Other device backup",
                isUsedBySyncOrBackup = true,
                backupId = backupId,
                deviceName = "Other device",
            )
            initViewModel(
                nodesResult = SyncFolderPickerNodesResult(
                    restrictedNodes = mapOf(restrictedNode.nodeId to restrictedNode),
                    isSelectEnabled = false,
                )
            )

            assertUiState(
                action = {
                    underTest.handleAction(
                        SelectSyncFolderAction.RestrictedFolderClicked(restrictedNode.nodeId)
                    )
                    underTest.handleAction(SelectSyncFolderAction.RemoveConnectionConfirmed)
                }
            ) { state ->
                val data = state.data()
                assertThat(data.removeConnectionNode).isNull()
                assertThat(data.warningEvent).isEqualTo(
                    triggered(
                        LocalizedText.StringRes(
                            sharedR.string.device_center_snackbar_message_connection_removed
                        )
                    )
                )
            }
            verify(syncFolderPickerHandler).removeFolderConnection(backupId)
        }

    @Test
    fun `test that an error is shown when the connection removal fails`() = runTest {
        val backupId = 555L
        val restrictedNode = SyncFolderPickerRestrictedNode(
            nodeId = NodeId(987L),
            name = "Other device backup",
            isUsedBySyncOrBackup = true,
            backupId = backupId,
            deviceName = "Other device",
        )
        initViewModel(
            nodesResult = SyncFolderPickerNodesResult(
                restrictedNodes = mapOf(restrictedNode.nodeId to restrictedNode),
                isSelectEnabled = false,
            )
        )
        doAnswer { throw RuntimeException("boom") }
            .whenever(syncFolderPickerHandler).removeFolderConnection(backupId)

        assertUiState(
            action = {
                underTest.handleAction(
                    SelectSyncFolderAction.RestrictedFolderClicked(restrictedNode.nodeId)
                )
                underTest.handleAction(SelectSyncFolderAction.RemoveConnectionConfirmed)
            }
        ) { state ->
            assertThat(state.data().warningEvent)
                .isEqualTo(triggered(LocalizedText.StringRes(sharedR.string.general_text_error)))
        }
    }

    @Test
    fun `test that processing is cleared when a warning is shown`() = runTest {
        initViewModel()
        whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
            .thenReturn("conflict")

        assertUiState(action = { selectCurrentFolder() }) { state ->
            assertThat(state.data().isProcessing).isFalse()
        }
    }

    @Test
    fun `test that the warning event is consumed when handled`() = runTest {
        initViewModel()
        whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
            .thenReturn("conflict")

        assertUiState(
            action = {
                selectCurrentFolder()
                underTest.handleAction(SelectSyncFolderAction.WarningEventConsumed)
            }
        ) { state ->
            assertThat(state.data().warningEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that the folder confirmed event is consumed when handled`() = runTest {
        initViewModel()
        whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
            .thenReturn(null)
        whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId)).thenReturn(null)
        whenever(syncFolderPickerHandler.saveSelectedFolder(rootFolderId)).thenReturn(true)

        assertUiState(
            action = {
                selectCurrentFolder()
                underTest.handleAction(SelectSyncFolderAction.FolderConfirmedEventConsumed)
            }
        ) { state ->
            assertThat(state.data().folderConfirmedEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that selecting saves the folder once the battery optimization dialog has been handled`() =
        runTest {
            initViewModel(batteryOptimizationGranted = false)
            whenever(getFeatureFlagValueUseCase(SyncFeatures.DisableBatteryOptimization))
                .thenReturn(true)
            whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.validateNodeSyncability(rootFolderId))
                .thenReturn(null)
            whenever(syncFolderPickerHandler.saveSelectedFolder(rootFolderId)).thenReturn(true)

            assertUiState(action = { selectCurrentFolder() }) { state ->
                assertThat(state.data().disableBatteryOptimizationsEvent).isEqualTo(triggered)
            }

            // Once the battery dialog has been handled, re-selecting saves the folder
            assertUiState(
                action = {
                    underTest.handleAction(
                        SelectSyncFolderAction.DisableBatteryOptimizationsHandled
                    )
                    selectCurrentFolder()
                }
            ) { state ->
                assertThat(state.data().folderConfirmedEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that the selection does nothing while the current folder is not resolved`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(null)
            whenever(
                syncFolderPickerHandler.monitorPickerNodes(any(), any(), anyOrNull())
            ).thenReturn(flowOf(SyncFolderPickerNodesResult()))
            underTest = SelectSyncFolderViewModel(
                args = SelectSyncFolderViewModel.Args(
                    folderHandle = SelectSyncFolderViewModel.INVALID_FOLDER_HANDLE,
                    isStopBackup = false,
                    stopBackupFolderName = null,
                ),
                getRootNodeIdUseCase = getRootNodeIdUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                syncFolderPickerHandler = syncFolderPickerHandler,
                syncPermissionsManager = syncPermissionsManager,
            )

            assertUiState(action = { selectCurrentFolder() }) { state ->
                assertThat(state).isEqualTo(SelectSyncFolderUiState.Loading)
            }

            verify(syncFolderPickerHandler, never()).getFolderUsageConflictMessage(any())
            verify(syncFolderPickerHandler, never()).saveSelectedFolder(any())
        }

    @Test
    fun `test that the picker stays loading when resolving the root folder fails`() = runTest {
        doAnswer { throw RuntimeException("boom") }.whenever(getRootNodeIdUseCase)()
        underTest = SelectSyncFolderViewModel(
            args = SelectSyncFolderViewModel.Args(
                folderHandle = SelectSyncFolderViewModel.INVALID_FOLDER_HANDLE,
                isStopBackup = false,
                stopBackupFolderName = null,
            ),
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            syncFolderPickerHandler = syncFolderPickerHandler,
            syncPermissionsManager = syncPermissionsManager,
        )

        assertUiState { state ->
            assertThat(state).isEqualTo(SelectSyncFolderUiState.Loading)
        }
        verifyNoInteractions(syncFolderPickerHandler)
    }

    @Test
    fun `test that the stop backup parameters are propagated when monitoring picker nodes`() =
        runTest {
            val folderName = "Backup folder"
            initViewModel(isStopBackup = true, stopBackupFolderName = folderName)

            assertUiState()

            verify(syncFolderPickerHandler).monitorPickerNodes(
                folderId = rootFolderId,
                isStopBackup = true,
                stopBackupFolderName = folderName,
            )
        }

    @Test
    fun `test that a conflicting folder shows the conflict snackbar and no permission dialog`() =
        runTest {
            // Conflict check runs before the permission flags are recorded, so a conflicting
            // folder never triggers the permission dialogs
            initViewModel(batteryOptimizationGranted = false)
            whenever(syncFolderPickerHandler.getFolderUsageConflictMessage(rootFolderId))
                .thenReturn("conflict")

            assertUiState(action = { selectCurrentFolder() }) { state ->
                val data = state.data()
                assertThat(data.disableBatteryOptimizationsEvent).isEqualTo(consumed)
                assertThat(data.warningEvent).isEqualTo(triggered(LocalizedText.Literal("conflict")))
            }
        }
}
