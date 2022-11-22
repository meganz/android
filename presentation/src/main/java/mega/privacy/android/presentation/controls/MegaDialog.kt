package mega.privacy.android.presentation.controls

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.presentation.theme.h6

/**
 * A reusable Dialog
 */
@Composable
fun MegaDialog(
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    onDismissRequest: () -> Unit,
    titleStringID: Int? = null,
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
            Column {
                // Dialog title
                if (titleStringID != null) {
                    Text(
                        text = stringResource(id = titleStringID),
                        style = h6,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
                    )
                }

                // Dialog body
                body()

                // Dialog button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 8.dp),
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