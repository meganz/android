package mega.privacy.android.app.presentation.tags

import mega.privacy.android.core.R as CoreR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
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
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionTextField
import mega.privacy.android.shared.original.core.ui.controls.textfields.transformations.PrefixTransformation
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import java.util.Locale

/**
 * Tags screen composable.
 */
@Composable
fun TagsScreen(
    addNodeTag: (String) -> Unit,
    consumeInfoMessage: () -> Unit,
    validateTagName: (String) -> Boolean,
    removeTag: (String) -> Unit,
    onBackPressed: () -> Unit,
    uiState: TagsUiState,
) {
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current
    EventEffect(event = uiState.informationMessage, onConsumed = consumeInfoMessage) { info ->
        scaffoldState.snackbarHostState.showSnackbar(info.getInfo(context))
    }
    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
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
            removeTag = removeTag,
            uiState = uiState,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsContent(
    addNodeTag: (String) -> Unit,
    validateTagName: (String) -> Boolean,
    removeTag: (String) -> Unit,
    uiState: TagsUiState,
    modifier: Modifier = Modifier,
) {
    var tag by rememberSaveable { mutableStateOf("") }

    fun addTag() {
        if (tag.isNotBlank() && uiState.isError.not()) {
            addNodeTag(tag)
            tag = ""
        }
    }

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
                onDone = { addTag() }
            ),
            showError = uiState.isError,
            onValueChange = {
                tag = it.removePrefix("#").lowercase(Locale.ROOT)
                validateTagName(tag)
            },
            title = stringResource(id = R.string.label_red),
            showUnderline = true,
        )

        if (tag.isNotBlank() && uiState.isError.not()) {
            TextMegaButton(
                contentPadding = PaddingValues(vertical = 8.dp),
                text = "Add \"#$tag\" tag",
                onClick = ::addTag,
                textAlign = TextAlign.Start
            )
        }

        if (uiState.tags.isNotEmpty()) {
            MegaText(
                modifier = Modifier.padding(vertical = 12.dp),
                text = "Existing Tags",
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(uiState.tags.size) { tag ->
                MegaChip(
                    selected = true,
                    text = "#${uiState.tags[tag]}",
                    contentDescription = "Tag Chip",
                    enabled = true,
                    onClick = { removeTag(uiState.tags[tag]) },
                    leadingIcon = CoreR.drawable.ic_filter_selected,
                )
            }
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
            removeTag = {},
            uiState = TagsUiState(tags = listOf("tag1", "tag2"))
        )
    }
}
