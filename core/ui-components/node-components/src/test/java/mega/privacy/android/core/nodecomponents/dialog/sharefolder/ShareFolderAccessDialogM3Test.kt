package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ShareFolderAccessDialogM3Test {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val shareAccessDialogViewModel = mock<ShareFolderAccessDialogViewModel>()

    @Test
    fun `test that 3 permission options are displayed in M3 access folder share dialog`() {
        composeTestRule.setContent {
            ShareFolderAccessDialogM3(
                handles = listOf(1234L, 2345L),
                contactData = listOf("sample.mega.co.nz", "test@mega.co.na"),
                isFromBackups = false,
                viewModel = shareAccessDialogViewModel,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.file_properties_shared_folder_read_only)
        ).assertExists()

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.file_properties_shared_folder_read_write)
        ).assertExists()

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.share_folder_dialog_full_access_radio_option)
        ).assertExists()
    }

    @Test
    fun `test that dialog title is displayed correctly`() {
        composeTestRule.setContent {
            ShareFolderAccessDialogM3(
                handles = listOf(1234L),
                contactData = listOf("test@mega.co.nz"),
                isFromBackups = false,
                viewModel = shareAccessDialogViewModel,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.dialog_select_permissions)
        ).assertExists()
    }

    @Test
    fun `test that cancel and ok buttons are displayed`() {
        composeTestRule.setContent {
            ShareFolderAccessDialogM3(
                handles = listOf(1234L),
                contactData = listOf("test@mega.co.nz"),
                isFromBackups = false,
                viewModel = shareAccessDialogViewModel,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.general_dialog_cancel_button)
        ).assertExists()

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.general_ok)
        ).assertExists()
    }
}
