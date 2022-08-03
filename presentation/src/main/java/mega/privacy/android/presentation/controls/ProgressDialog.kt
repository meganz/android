package mega.privacy.android.presentation.controls

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.teal_200
import mega.privacy.android.presentation.theme.teal_300
import mega.privacy.android.presentation.theme.white_alpha_054

@Composable
fun ProgressDialog(
    title: String,
    progress: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelButtonText: String,
) {
    Dialog(
        onDismissRequest = {},
        DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 24.dp,
                    top = 24.dp,
                    end = 24.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.subtitle1,
                    color = if (!MaterialTheme.colors.isLight) white_alpha_054 else grey_alpha_054
                )
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 32.dp),
                    color = if (!MaterialTheme.colors.isLight) teal_200 else teal_300
                )
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text(
                        text = cancelButtonText,
                        color = if (!MaterialTheme.colors.isLight) teal_200 else teal_300
                    )
                }
            }
        }
    }
}

@ShowkaseComposable("Progress Dialog", "Dialogs")
@Composable
fun ShowkasePreviewProgressDialog() = PreviewProgressDialog()

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewProgressDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ProgressDialog(
            title = "Title goes here",
            progress = 0.3f,
            onCancel = {},
            cancelButtonText = "Cancel"
        )
    }
}