package mega.privacy.android.app.presentation.node.dialogs.sharefolder.access

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
class ShareFolderAccessDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val shareAccessDialogViewModel = mock<ShareFolderAccessDialogViewModel>()

    @Test
    fun `test that 3 options will be displayed in access folder share dialog`() {
        composeTestRule.setContent {
            ShareFolderAccessDialog(
                handles = listOf(1234L, 2345L),
                contactData = listOf("sample.mega.co.nz", "test@mega.co.na"),
                onDismiss = {},
                viewModel = shareAccessDialogViewModel
            )
        }
        composeTestRule.onNodeWithText(
            fromId(
                R.string.file_properties_shared_folder_read_only,
            )
        ).assertExists()
        composeTestRule.onNodeWithText(
            fromId(
                R.string.file_properties_shared_folder_read_write,
            )
        ).assertExists()
        composeTestRule.onNodeWithText(
            fromId(
                R.string.file_properties_shared_folder_full_access,
            )
        ).assertExists()
    }
}