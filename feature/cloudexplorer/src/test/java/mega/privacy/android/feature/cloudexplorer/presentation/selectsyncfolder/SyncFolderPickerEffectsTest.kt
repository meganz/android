package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class SyncFolderPickerEffectsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val syncPermissionsManager = mock<SyncPermissionsManager>()

    private val actions = mutableListOf<SelectSyncFolderAction>()
    private var folderConfirmedCount = 0

    private fun dataState(
        removeConnectionNode: SyncFolderPickerRestrictedNode? = null,
        disableBatteryOptimizationsEvent: StateEvent = consumed,
        warningEvent: StateEventWithContent<LocalizedText> = consumed(),
        folderConfirmedEvent: StateEvent = consumed,
    ) = SelectSyncFolderUiState.Data(
        currentFolderId = NodeId(1L),
        restrictedNodes = emptyMap(),
        isSelectEnabled = true,
        removeConnectionNode = removeConnectionNode,
        isProcessing = false,
        disableBatteryOptimizationsEvent = disableBatteryOptimizationsEvent,
        warningEvent = warningEvent,
        folderConfirmedEvent = folderConfirmedEvent,
    )

    private fun setContent(uiState: SelectSyncFolderUiState) {
        composeTestRule.setContent {
            SyncFolderPickerEffects(
                uiState = uiState,
                syncPermissionsManager = syncPermissionsManager,
                onAction = { actions.add(it) },
                onFolderConfirmed = { folderConfirmedCount++ },
            )
        }
    }

    @Test
    fun `test that the battery optimization dialog is shown when its event is triggered`() {
        setContent(dataState(disableBatteryOptimizationsEvent = triggered))

        composeTestRule
            .onNodeWithText(context.getString(sharedR.string.sync_dialog_battery_optimization_title))
            .assertIsDisplayed()
        assertThat(actions)
            .contains(SelectSyncFolderAction.DisableBatteryOptimizationsEventConsumed)
    }

    @Test
    fun `test that the battery optimization dialog is not shown when its event is consumed`() {
        setContent(dataState())

        composeTestRule
            .onNodeWithText(context.getString(sharedR.string.sync_dialog_battery_optimization_title))
            .assertDoesNotExist()
    }

    @Test
    fun `test that the remove connection dialog is shown when a node is set in the state`() {
        val node = SyncFolderPickerRestrictedNode(
            nodeId = NodeId(2L),
            name = "Other device backup",
            isUsedBySyncOrBackup = true,
            backupId = 5L,
            deviceName = "Other device",
        )

        setContent(dataState(removeConnectionNode = node))

        composeTestRule
            .onNodeWithText(context.getString(sharedR.string.sync_folder_connection_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that onFolderConfirmed is invoked when the folder confirmed event is triggered`() {
        setContent(dataState(folderConfirmedEvent = triggered))
        composeTestRule.waitForIdle()

        assertThat(folderConfirmedCount).isEqualTo(1)
        assertThat(actions).contains(SelectSyncFolderAction.FolderConfirmedEventConsumed)
    }

    @Test
    fun `test that the warning event is consumed when triggered`() {
        setContent(
            dataState(
                warningEvent = triggered(
                    LocalizedText.StringRes(sharedR.string.general_text_error)
                )
            )
        )
        composeTestRule.waitForIdle()

        assertThat(actions).contains(SelectSyncFolderAction.WarningEventConsumed)
    }

    @Test
    fun `test that nothing is shown in the loading state`() {
        setContent(SelectSyncFolderUiState.Loading)

        composeTestRule
            .onNodeWithText(context.getString(sharedR.string.sync_dialog_battery_optimization_title))
            .assertDoesNotExist()
        assertThat(actions).isEmpty()
        assertThat(folderConfirmedCount).isEqualTo(0)
    }
}
