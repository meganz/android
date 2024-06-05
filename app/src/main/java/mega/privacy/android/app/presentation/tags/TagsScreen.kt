package mega.privacy.android.app.presentation.tags

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionTextField
import mega.privacy.android.shared.original.core.ui.controls.textfields.transformations.PrefixTransformation
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Tags screen composable.
 */
@Composable
fun TagsScreen(
    addNodeTag: (String) -> Unit,
    consumeInfoMessage: () -> Unit,
    validateTagName: (String) -> Boolean,
    onBackPressed: () -> Unit,
    uiState: TagsUiState,
) {
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current
    EventEffect(event = uiState.informationMessage, onConsumed = consumeInfoMessage) { info ->
        scaffoldState.snackbarHostState.showSnackbar(info.getInfo(context))
    }
    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = "Tags",
                elevation = 0.dp,
                onNavigationPressed = { onBackPressed() },
            )
        },
    ) { paddingValues ->
        TagsContent(
            modifier = Modifier.padding(paddingValues),
            addNodeTag = addNodeTag,
            validateTagName = validateTagName,
            uiState = uiState
        )
    }
}

@Composable
private fun TagsContent(
    addNodeTag: (String) -> Unit,
    validateTagName: (String) -> Boolean,
    uiState: TagsUiState,
    modifier: Modifier = Modifier,
) {
    var tag by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        MegaText(
            text = "Tag",
            textColor = if (uiState.isError) TextColor.Error else TextColor.Accent,
            style = MaterialTheme.typography.caption,
        )
        // Tags content
        GenericDescriptionTextField(
            modifier = Modifier.padding(bottom = 4.dp),
            visualTransformation = PrefixTransformation("#"),
            value = tag,
            imeAction = ImeAction.Done,
            supportingText = uiState.message,
            keyboardActions = KeyboardActions(
                onDone = {
                    if (validateTagName(tag)) {
                        addNodeTag(tag)
                        tag = ""
                    }
                }

            ),
            showError = uiState.isError,
            onValueChange = {
                tag = it
                validateTagName(it)
            },
            title = stringResource(id = R.string.label_red),
            showUnderline = true,
        )

        if (tag.isNotBlank() && uiState.isError.not()) {
            TextMegaButton(
                contentPadding = PaddingValues(vertical = 8.dp),
                text = "Add \"#$tag\" tag",
                onClick = { addNodeTag(tag) },
                textAlign = TextAlign.Start
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun TagsScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TagsScreen(
            addNodeTag = {},
            consumeInfoMessage = {},
            validateTagName = { it.isNotEmpty() },
            onBackPressed = {},
            uiState = TagsUiState(tags = listOf("tag1", "tag2"))
        )
    }
}
