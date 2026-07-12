package mega.privacy.android.app.presentation.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.DefaultChipStyle
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.inputfields.HelpTextInfo
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.tags.TagsActivity.Companion.MAX_TAGS_PER_NODE
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.textfields.transformations.PrefixTransformation
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.NodeInfoTagsAddedEvent
import mega.privacy.mobile.analytics.event.NodeInfoTagsRemovedEvent

/**
 * Stateful entry point for the tags screen: creates the [TagsViewModel] for [nodeHandle] and hosts
 * the [TagsScreen]. Shared by both the legacy [TagsActivity] and the Navigation3 tags destination.
 *
 * @param nodeHandle the node handle whose tags are shown/edited
 * @param onBackPressed invoked when the back navigation is triggered
 */
@Composable
fun TagsRoute(
    nodeHandle: Long,
    onBackPressed: () -> Unit,
) {
    val viewModel = hiltViewModel<TagsViewModel, TagsViewModel.Factory> {
        it.create(nodeHandle)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TagsScreen(
        consumeInfoMessage = viewModel::consumeInfoMessage,
        validateTagName = viewModel::validateTagName,
        addOrRemoveTag = viewModel::addOrRemoveTag,
        onBackPressed = onBackPressed,
        consumeMaxTagsError = viewModel::consumeMaxTagsError,
        uiState = uiState,
        consumeTagsUpdated = viewModel::consumeTagsUpdatedEvent,
    )
}

/**
 * Tags screen composable.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    consumeInfoMessage: () -> Unit,
    validateTagName: (String) -> Unit,
    addOrRemoveTag: (String) -> Unit,
    onBackPressed: () -> Unit,
    consumeMaxTagsError: () -> Unit,
    uiState: TagsUiState,
    consumeTagsUpdated: () -> Unit,
) {
    val snackbarHostState = LocalSnackBarHostState.current ?: remember { SnackbarHostState() }
    val context = LocalContext.current
    val maxTagsError =
        stringResource(id = sharedR.string.add_tags_error_max_tags, MAX_TAGS_PER_NODE)
    val coroutineScope = rememberCoroutineScope()

    // Provide to support legacy activity wrapper
    CompositionLocalProvider(LocalSnackBarHostState provides snackbarHostState) {
        MegaScaffoldWithTopAppBarScrollBehavior(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .semantics { testTagsAsResourceId = true },
            topBar = {
                MegaTopAppBar(
                    modifier = Modifier.testTag(TAGS_SCREEN_APP_BAR),
                    title = stringResource(id = sharedR.string.add_tags_page_title_label),
                    navigationType = AppBarNavigationType.Back(onBackPressed),
                )
            },
        ) { paddingValues ->
            EventEffect(
                event = uiState.informationMessage,
                onConsumed = consumeInfoMessage
            ) { info ->
                snackbarHostState.showAutoDurationSnackbar(info.getInfo(context))
            }
            EventEffect(event = uiState.showMaxTagsError, onConsumed = consumeMaxTagsError) {
                snackbarHostState.showAutoDurationSnackbar(maxTagsError)
            }
            TagsContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(TAGS_SCREEN_CONTENTS_LABEL),
                validateTagName = validateTagName,
                addOrRemoveTag = addOrRemoveTag,
                consumeTagsUpdated = consumeTagsUpdated,
                uiState = uiState,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsContent(
    validateTagName: (String) -> Unit,
    addOrRemoveTag: (String) -> Unit,
    uiState: TagsUiState,
    modifier: Modifier = Modifier,
    consumeTagsUpdated: () -> Unit,
) {
    var tag by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun addTag(selectedTag: String, newTag: Boolean = false) {
        if (selectedTag.isNotBlank() && (!newTag || !uiState.isError)) {
            addOrRemoveTag(selectedTag)
        }
    }

    EventEffect(event = uiState.tagsUpdatedEvent, onConsumed = consumeTagsUpdated) {
        tag = ""
        val event = if (it == TagUpdate.ADD) NodeInfoTagsAddedEvent else NodeInfoTagsRemovedEvent
        if (it == TagUpdate.ADD) focusManager.clearFocus()
        Analytics.tracker.trackEvent(event)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextInputField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAGS_SCREEN_ADD_TAGS_TEXT_FIELD),
            placeholder = stringResource(id = sharedR.string.add_tags_placeholder_label),
            text = tag,
            // Only show the placeholder ("#Add tags") while the field is empty AND unfocused. Once
            // focused or typing, fix the "#" prefix (which puts the cursor right after it); applying
            // the prefix while empty+unfocused would make the field look non-empty and hide the placeholder.
            visualTransformation = if (tag.isEmpty() && !isFocused) {
                VisualTransformation.None
            } else {
                PrefixTransformation("#")
            },
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            onValueChanged = {
                tag = it.removePrefix("#").lowercase()
                validateTagName(tag)
            },
            onFocusChanged = { isFocused = it },
            errorText = uiState.message?.takeIf { uiState.isError },
        )

        if (!uiState.isError) {
            HelpTextInfo(
                modifier = Modifier.testTag(TAGS_SCREEN_ADD_TAGS_LABEL),
                text = stringResource(id = sharedR.string.add_tags_label_tag_description),
            )
        }

        if (tag.isNotBlank() && !uiState.isError && !uiState.tags.contains(tag)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAGS_SCREEN_ADD_TAGS_BUTTON)
                    .clickable { addTag(selectedTag = tag, newTag = true) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MegaIcon(
                    modifier = Modifier.size(24.dp),
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
                    tint = IconColor.Primary,
                    contentDescription = null,
                )
                MegaText(
                    text = stringResource(id = sharedR.string.add_tags_button_label_add, tag),
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                )
            }
        }

        if (uiState.tags.isNotEmpty()) {
            MegaText(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag(TAGS_SCREEN_EXISTING_TAGS_LABEL),
                text = stringResource(id = sharedR.string.add_tags_label_existing_tags),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.titleSmall,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.tags.forEach { tag ->
                val isSelected = uiState.nodeTags.contains(tag)
                MegaChip(
                    modifier = Modifier.testTag(TAGS_SCREEN_TAG_CHIP),
                    selected = isSelected,
                    content = "#$tag",
                    style = DefaultChipStyle,
                    leadingPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
                        .takeIf { isSelected },
                    onClick = {
                        addTag(tag)
                        Analytics.tracker.trackEvent(NodeInfoTagsRemovedEvent)
                    },
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun TagsScreenPreview() {
    AndroidThemeForPreviews {
        TagsScreen(
            consumeInfoMessage = {},
            validateTagName = {},
            addOrRemoveTag = {},
            onBackPressed = {},
            consumeMaxTagsError = {},
            uiState = TagsUiState(
                tags = persistentListOf("marketing", "2026", "documentation", "promo", "mega"),
                nodeTags = persistentListOf("marketing"),
            ),
            consumeTagsUpdated = {},
        )
    }
}

internal const val TAGS_SCREEN_APP_BAR = "tags_screen:tags_app_bar"
internal const val TAGS_SCREEN_CONTENTS_LABEL = "tags_screen:contents_label_tag"
internal const val TAGS_SCREEN_ADD_TAGS_LABEL = "tags_screen:add_tags_text_label_tag"
internal const val TAGS_SCREEN_ADD_TAGS_TEXT_FIELD = "tags_screen:add_tags_text_field"
internal const val TAGS_SCREEN_ADD_TAGS_BUTTON = "tags_screen:add_tags_button"
internal const val TAGS_SCREEN_EXISTING_TAGS_LABEL = "tags_screen:existing_tags_label"
internal const val TAGS_SCREEN_TAG_CHIP = "tags_screen:tag_chip"
