package mega.privacy.android.core.ui.controls.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.teal_300

/**
 *  A common loading dialog with a [MegaCircularProgressIndicator] and text.
 */
@Composable
fun LoadingDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String,
    dialogProperties: DialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    ),
) {
    Dialog(
        onDismissRequest = {},
        properties = dialogProperties
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.textColorSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .conditional(
                            title
                                .isNullOrBlank()
                                .not()
                        ) {
                            padding(top = 32.dp)
                        }
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    MegaCircularProgressIndicator()

                    Text(
                        text = text,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Preview Loading Dialog
 */
@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewLoading"
)
@Composable
fun PreviewLoadingDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LoadingDialog(text = "Loading..")
    }
}

/**
 * Preview Loading Dialog With Title
 */
@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewLoadingWithTitle"
)
@Composable
fun PreviewLoadingDialogWithTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LoadingDialog(title = "Title", text = "Loading..")
    }
}


