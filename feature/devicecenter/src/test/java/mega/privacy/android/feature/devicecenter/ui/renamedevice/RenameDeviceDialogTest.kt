package mega.privacy.android.feature.devicecenter.ui.renamedevice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.feature.devicecenter.ui.renamedevice.model.RenameDeviceState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [RenameDeviceDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class RenameDeviceDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val renameDeviceViewModel: RenameDeviceViewModel = mock()

    @Test
    fun `test that the rename device dialog is shown`() {
        val deviceId = "12345-6789"
        val oldDeviceName = "Old Device Name"
        val uiState = RenameDeviceState(
            renameSuccessfulEvent = consumed,
        )

        renameDeviceViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(uiState))
        }
        composeTestRule.setContent {
            RenameDeviceDialog(
                renameDeviceViewModel = renameDeviceViewModel,
                deviceId = deviceId,
                oldDeviceName = oldDeviceName,
                onRenameSuccessful = {},
                onRenameCancelled = {},
            )
        }
        composeTestRule.onNodeWithTag(RENAME_DEVICE_DIALOG_TAG).assertIsDisplayed()
    }
}