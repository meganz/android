package mega.privacy.android.shared.nodes.namecollision.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.consumed
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.indicators.LargeSpinnerIndicator
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.shared.nodes.namecollision.FileVersioningInfo
import mega.privacy.android.shared.nodes.namecollision.NameCollisionType
import mega.privacy.android.shared.nodes.namecollision.NameCollisionUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The redesigned duplicate items screen: one duplicate at a time, presented as a list of mutually
 * exclusive options (replace or merge, keep both, skip) confirmed by a single anchored action
 * button. The chosen option and the apply-to-all choice reset with every duplicate presented.
 *
 * @param uiState               Current [NameCollisionUiState].
 * @param onReplaceUpdateMerge  Replace (file), update (versioned file) or merge (folder) the existing
 *                              node. The flag is true when "apply to all" is checked.
 * @param onCancel              Skip the duplicate, keeping the existing node. The flag is true when
 *                              "apply to all" is checked.
 * @param onRename              Keep both by renaming the incoming item. The flag is true when "apply
 *                              to all" is checked.
 * @param onBack                Close the screen without resolving the duplicate.
 */
@Composable
fun DuplicateItemsScreen(
    uiState: NameCollisionUiState,
    onReplaceUpdateMerge: (applyForAll: Boolean) -> Unit,
    onCancel: (applyForAll: Boolean) -> Unit,
    onRename: (applyForAll: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collisionHandle = uiState.currentCollision?.nameCollision?.collisionHandle
    var selectedOption by rememberSaveable(collisionHandle, uiState.currentCollisionIndex) {
        mutableStateOf<DuplicateItemOption?>(null)
    }
    var applyToAll by rememberSaveable(collisionHandle, uiState.currentCollisionIndex) {
        mutableStateOf(false)
    }

    DuplicateItemsScreenContent(
        uiState = uiState,
        selectedOption = selectedOption,
        onOptionSelected = { selectedOption = it },
        applyToAll = applyToAll,
        onApplyToAllChanged = { applyToAll = it },
        onPrimaryClick = {
            when (selectedOption) {
                DuplicateItemOption.Replace -> onReplaceUpdateMerge(applyToAll)
                DuplicateItemOption.KeepBoth -> onRename(applyToAll)
                DuplicateItemOption.Skip -> onCancel(applyToAll)
                null -> Unit
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * The resolutions the duplicate items screen offers for the duplicate being presented.
 */
internal enum class DuplicateItemOption { Replace, KeepBoth, Skip }

@Composable
internal fun DuplicateItemsScreenContent(
    uiState: NameCollisionUiState,
    selectedOption: DuplicateItemOption?,
    onOptionSelected: (DuplicateItemOption) -> Unit,
    applyToAll: Boolean,
    onApplyToAllChanged: (Boolean) -> Unit,
    onPrimaryClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collision = uiState.currentCollision?.nameCollision

    MegaScaffold(
        modifier = modifier.testTag(DUPLICATE_ITEMS_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                title = stringResource(
                    when (collision?.isFile) {
                        true -> sharedR.string.name_collision_file_exists_title
                        false -> sharedR.string.name_collision_folder_exists_title
                        null -> sharedR.string.title_duplicated_items
                    }
                ),
                navigationType = AppBarNavigationType.Close(onBack),
                subtitle = stringResource(
                    sharedR.string.name_collision_duplicate_counter,
                    uiState.currentCollisionIndex,
                    uiState.currentTypeCollisionCount,
                ).takeIf { uiState.currentTypeCollisionCount > 1 },
            )
        },
        bottomBar = {
            if (collision != null) {
                InlineAnchoredButtonGroup(
                    primaryButtonText = stringResource(primaryButtonLabel(collision)),
                    textOnlyButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                    onPrimaryButtonClick = onPrimaryClick,
                    onTextOnlyButtonClick = onBack,
                    modifier = Modifier.testTag(DUPLICATE_ITEMS_BUTTON_GROUP_TAG),
                    primaryButtonEnabled = selectedOption != null,
                )
            }
        },
    ) { padding ->
        if (collision == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LargeSpinnerIndicator(modifier = Modifier.testTag(DUPLICATE_ITEMS_LOADING_TAG))
            }
        } else {
            DuplicateItemsOptions(
                collision = collision,
                fileVersioningInfo = uiState.fileVersioningInfo,
                pendingForType = if (collision.isFile) {
                    uiState.pendingFileCollisions
                } else {
                    uiState.pendingFolderCollisions
                },
                selectedOption = selectedOption,
                onOptionSelected = onOptionSelected,
                applyToAll = applyToAll,
                onApplyToAllChanged = onApplyToAllChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun DuplicateItemsOptions(
    collision: NameCollision,
    fileVersioningInfo: FileVersioningInfo?,
    pendingForType: Int,
    selectedOption: DuplicateItemOption?,
    onOptionSelected: (DuplicateItemOption) -> Unit,
    applyToAll: Boolean,
    onApplyToAllChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFile = collision.isFile
    val collisionType = collision.toNameCollisionType()

    Column(modifier = modifier) {
        MegaText(
            text = stringResource(
                sharedR.string.name_collision_already_exists_body,
                collision.name.truncateMiddleKeepingExtension(isFile),
            ),
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(DUPLICATE_ITEMS_BODY_TAG),
        )

        if (fileVersioningInfo?.canReplaceOrUpdate == true) {
            DuplicateItemOptionRow(
                option = DuplicateItemOption.Replace,
                title = stringResource(
                    if (isFile) {
                        sharedR.string.name_collision_option_replace_file
                    } else {
                        sharedR.string.name_collision_option_merge_folders
                    }
                ),
                subtitle = stringResource(
                    when {
                        !isFile -> sharedR.string.name_collision_option_merge_folders_subtitle
                        fileVersioningInfo.isFileVersioningEnabled ->
                            sharedR.string.name_collision_option_replace_file_versioning_subtitle

                        else -> sharedR.string.name_collision_option_replace_file_subtitle
                    }
                ),
                selected = selectedOption == DuplicateItemOption.Replace,
                onSelected = onOptionSelected,
                modifier = Modifier.testTag(DUPLICATE_ITEMS_OPTION_REPLACE_TAG),
            )
        }

        if (isFile || collisionType == NameCollisionType.UPLOAD) {
            DuplicateItemOptionRow(
                option = DuplicateItemOption.KeepBoth,
                title = stringResource(
                    if (isFile) {
                        sharedR.string.name_collision_option_keep_both_files
                    } else {
                        sharedR.string.name_collision_option_keep_both_folders
                    }
                ),
                subtitle = stringResource(
                    when {
                        !isFile -> sharedR.string.name_collision_option_keep_both_folders_subtitle
                        collisionType == NameCollisionType.MOVE ->
                            sharedR.string.name_collision_option_keep_both_files_move_subtitle

                        else -> sharedR.string.name_collision_option_keep_both_files_subtitle
                    },
                    collision.renameName.orEmpty(),
                ),
                selected = selectedOption == DuplicateItemOption.KeepBoth,
                onSelected = onOptionSelected,
                modifier = Modifier.testTag(DUPLICATE_ITEMS_OPTION_KEEP_BOTH_TAG),
            )
        }

        DuplicateItemOptionRow(
            option = DuplicateItemOption.Skip,
            title = stringResource(
                if (isFile) {
                    sharedR.string.name_collision_option_skip_file
                } else {
                    sharedR.string.skip_folder
                }
            ),
            subtitle = null,
            selected = selectedOption == DuplicateItemOption.Skip,
            onSelected = onOptionSelected,
            modifier = Modifier.testTag(DUPLICATE_ITEMS_OPTION_SKIP_TAG),
        )

        if (pendingForType > 0) {
            val applyCount = pendingForType + 1
            SubtleDivider()
            FlexibleLineListItem(
                modifier = Modifier.testTag(DUPLICATE_ITEMS_APPLY_TO_ALL_TAG),
                title = pluralStringResource(
                    sharedR.plurals.name_collision_apply_to_all,
                    applyCount,
                    applyCount,
                ),
                trailingElement = {
                    Checkbox(
                        checked = applyToAll,
                        onCheckStateChanged = onApplyToAllChanged,
                        modifier = Modifier.testTag(DUPLICATE_ITEMS_APPLY_TO_ALL_CHECKBOX_TAG),
                    )
                },
                onClickListener = { onApplyToAllChanged(!applyToAll) },
            )
        }
    }
}

@Composable
private fun DuplicateItemOptionRow(
    option: DuplicateItemOption,
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelected: (DuplicateItemOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlexibleLineListItem(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        trailingElement = {
            MegaRadioButton(
                identifier = option,
                onOptionSelected = { onSelected(option) },
                selected = selected,
            )
        },
        onClickListener = { onSelected(option) },
    )
}

@StringRes
private fun primaryButtonLabel(collision: NameCollision): Int = when (collision) {
    is FileNameCollision -> sharedR.string.general_upload_label
    is NodeNameCollision.Chat, is NodeNameCollision.FileLink ->
        sharedR.string.name_collision_import_button

    is NodeNameCollision.Default ->
        if (collision.type == NodeNameCollisionType.MOVE) {
            sharedR.string.general_move
        } else {
            sharedR.string.general_copy
        }
}

const val DUPLICATE_ITEMS_SCREEN_TAG = "duplicate_items_screen:screen"
const val DUPLICATE_ITEMS_LOADING_TAG = "duplicate_items_screen:loading"
const val DUPLICATE_ITEMS_BODY_TAG = "duplicate_items_screen:body"
const val DUPLICATE_ITEMS_OPTION_REPLACE_TAG = "duplicate_items_screen:option_replace"
const val DUPLICATE_ITEMS_OPTION_KEEP_BOTH_TAG = "duplicate_items_screen:option_keep_both"
const val DUPLICATE_ITEMS_OPTION_SKIP_TAG = "duplicate_items_screen:option_skip"
const val DUPLICATE_ITEMS_APPLY_TO_ALL_TAG = "duplicate_items_screen:apply_to_all"
const val DUPLICATE_ITEMS_APPLY_TO_ALL_CHECKBOX_TAG = "duplicate_items_screen:apply_to_all_checkbox"
const val DUPLICATE_ITEMS_BUTTON_GROUP_TAG = "duplicate_items_screen:button_group"

@CombinedThemePreviews
@Composable
private fun DuplicateItemsScreenPreview() {
    AndroidThemeForPreviews {
        DuplicateItemsScreenContent(
            uiState = NameCollisionUiState(
                currentCollision = NodeNameCollisionResult(
                    nameCollision = FileNameCollision(
                        collisionHandle = 1L,
                        name = "IMG000012.jpeg",
                        size = 57_344L,
                        lastModified = 1_695_600_000L,
                        parentHandle = 10L,
                        isFile = true,
                        renameName = "IMG000012 (1).jpeg",
                        pitagTrigger = PitagTrigger.NotApplicable,
                        path = UriPath("file:///storage/emulated/0/DCIM/IMG000012.jpeg"),
                    ),
                    collisionName = "IMG000012.jpeg",
                    collisionSize = 40_960L,
                    collisionFolderContent = null,
                    collisionLastModified = 1_695_600_000L,
                    collisionThumbnail = null,
                    thumbnail = null,
                    choice = null,
                ),
                fileVersioningInfo = FileVersioningInfo(
                    isFileVersioningEnabled = false,
                    nameCollisionType = NameCollisionType.UPLOAD,
                    isFile = true,
                    canReplaceOrUpdate = true,
                ),
                isCopyToOrigin = false,
                pendingFileCollisions = 2,
                pendingFolderCollisions = 0,
                currentCollisionIndex = 1,
                currentTypeCollisionCount = 3,
                hasPendingCollisions = true,
                uploadedFilesCount = 0,
                uploadEvent = consumed(),
                outcome = consumed(),
                dismissEvent = consumed,
            ),
            selectedOption = DuplicateItemOption.Replace,
            onOptionSelected = {},
            applyToAll = false,
            onApplyToAllChanged = {},
            onPrimaryClick = {},
            onBack = {},
        )
    }
}
