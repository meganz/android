package mega.privacy.android.app.presentation.controls

import android.content.res.Configuration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.ThemeMode
import mega.privacy.android.app.presentation.theme.AndroidTheme

@Composable
fun MegaTextField(
    label: String,
    description: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = description,
        onValueChange = onTextChanged,
        label = {
            Text(text = label)
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = colorResource(id = R.color.colorSurface),
            unfocusedIndicatorColor = colorResource(id = R.color.grey_alpha_012),
            textColor = MaterialTheme.colors.onSurface,
            cursorColor = MaterialTheme.colors.secondary,
            focusedLabelColor = MaterialTheme.colors.secondary,
            focusedIndicatorColor = MaterialTheme.colors.secondary,
        ),
        modifier = modifier
    )
}

@ShowkaseComposable("Text field - Generic", "Text fields")
@Composable
fun ShowkasePreviewTextField() = PreviewTextField()

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewTextField() {
    var content by remember {
        mutableStateOf("")
    }
    AndroidTheme(mode = ThemeMode.System) {
        MegaTextField(
            label = "This is the label",
            description = content,
            onTextChanged = { content = it },
        )
    }
}