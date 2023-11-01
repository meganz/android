package mega.privacy.android.legacy.core.ui.controls.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.theme.h6

/**
 * A reusable Dialog
 */
@Deprecated(
    "MegaDialog has been deprecated in favour of Specific dialogs in our Design system: " +
            "https://www.figma.com/file/Ki502d51Imw3nzscZe7Slt/Components---Android?type=design&node-id=337-1713&t=LMP26QoxVcHMqUxf-0" +
            "check that the Dialog you need is defined in our Design system and if it's already implemented (like [MegaAlertDialog] or [ConfirmationWithRadioButtonsDialog])," +
            "If the dialog you need is not implemented you should implement it first:" +
            "https://confluence.developers.mega.co.nz/display/MOB/Implementing+new+designs",
)
@Composable
fun MegaDialog(
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    onDismissRequest: () -> Unit,
    titleAlign: TextAlign? = null,
    titleString: String? = null,
    fontWeight: FontWeight? = null,
    body: @Composable (() -> Unit) = {},
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = modifier,
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(start = 24.dp, top = 16.dp, end = 24.dp),
                ) {
                    // Dialog title
                    if (titleString != null) {
                        Text(
                            textAlign = titleAlign ?: TextAlign.Center,
                            text = titleString,
                            style = h6,
                            fontWeight = fontWeight,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    // Dialog body
                    body()
                }

                // Dialog button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 32.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Dialog dismiss button
                    dismissButton()

                    // Dialog confirm button
                    confirmButton()
                }
            }
        }
    }
}