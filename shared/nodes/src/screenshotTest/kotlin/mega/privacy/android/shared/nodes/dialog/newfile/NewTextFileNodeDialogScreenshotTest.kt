package mega.privacy.android.shared.nodes.dialog.newfile

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId

class NewTextFileNodeDialogScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewTextFileNodeDialogWithRichTextOption() {
        AndroidThemeForPreviews {
            NewTextFileNodeDialog(
                uiState = NewTextFileNodeDialogUiState.Data(
                    parentNodeId = NodeId(-1L),
                    fileName = "New file.txt",
                    fileNameException = null,
                    validationSuccessEvent = consumed(),
                    showRichTextOption = true,
                ),
                title = "New text file",
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }
    }
}
