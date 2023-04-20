package mega.privacy.android.core.ui.controls.textfields

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Error text in text field
 *
 * @param errorTextId   String resource
 */
@Composable
fun ErrorTextTextField(@StringRes errorTextId: Int) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        text = stringResource(id = errorTextId),
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.error,
        textAlign = TextAlign.Start
    )
}