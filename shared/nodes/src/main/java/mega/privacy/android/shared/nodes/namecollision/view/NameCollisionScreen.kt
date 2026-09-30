package mega.privacy.android.shared.nodes.namecollision.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.consumed
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.card.RoundCard
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.indicators.LargeSpinnerIndicator
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.nodes.namecollision.FileVersioningInfo
import mega.privacy.android.shared.nodes.namecollision.NameCollisionType
import mega.privacy.android.shared.nodes.namecollision.NameCollisionUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Stateless Compose screen that lets the user resolve a name collision by replacing/updating/merging,
 * renaming, or skipping the item being uploaded, copied or moved. It renders entirely from [uiState]
 * and reports the user's choice through the callbacks.
 *
 * @param uiState               Current [NameCollisionUiState].
 * @param onReplaceUpdateMerge  Replace (file), update (versioned file) or merge (folder) the existing
 *                              node. The flag is true when "apply to all" is checked.
 * @param onCancel              Skip the collision, keeping the existing node. The flag is true when
 *                              "apply to all" is checked.
 * @param onRename              Keep both by renaming the incoming item. The flag is true when "apply
 *                              to all" is checked.
 * @param onLearnMore           Open the file-versioning help article.
 * @param onBack                Navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameCollisionScreen(
    uiState: NameCollisionUiState,
    onReplaceUpdateMerge: (applyForAll: Boolean) -> Unit,
    onCancel: (applyForAll: Boolean) -> Unit,
    onRename: (applyForAll: Boolean) -> Unit,
    onLearnMore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.testTag(NAME_COLLISION_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.title_duplicated_items),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
    ) { padding ->
        val collisionResult = uiState.currentCollision
        if (collisionResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LargeSpinnerIndicator(modifier = Modifier.testTag(NAME_COLLISION_LOADING_TAG))
            }
        } else {
            NameCollisionContent(
                collisionResult = collisionResult,
                fileVersioningInfo = uiState.fileVersioningInfo,
                pendingCollisions = if (collisionResult.nameCollision.isFile) {
                    uiState.pendingFileCollisions
                } else {
                    uiState.pendingFolderCollisions
                },
                onReplaceUpdateMerge = onReplaceUpdateMerge,
                onCancel = onCancel,
                onRename = onRename,
                onLearnMore = onLearnMore,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun NameCollisionContent(
    collisionResult: NodeNameCollisionResult,
    fileVersioningInfo: FileVersioningInfo?,
    pendingCollisions: Int,
    onReplaceUpdateMerge: (Boolean) -> Unit,
    onCancel: (Boolean) -> Unit,
    onRename: (Boolean) -> Unit,
    onLearnMore: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val collision = collisionResult.nameCollision
    val isFile = collision.isFile
    val name = collision.name
    val collisionType = collision.toNameCollisionType()
    val fileTypeIconMapper = remember { FileTypeIconMapper() }
    val defaultIcon = if (isFile) {
        fileTypeIconMapper(name.substringAfterLast('.', ""))
    } else {
        IconPackR.drawable.ic_folder_medium_solid
    }

    val locale = LocalConfiguration.current.locales[0]
    val formattedDate = collisionResult.collisionLastModified
        ?.let { formatModifiedDate(locale, it) }

    var applyForAll by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SpannedText(
            value = stringResource(
                id = if (isFile) {
                    sharedR.string.file_already_exists_in_location
                } else {
                    sharedR.string.folder_already_exists_in_location
                },
                name,
            ),
            baseStyle = AppTheme.typography.bodyLarge,
            baseTextColor = TextColor.Secondary,
            spanStyles = mapOf(
                SpanIndicator('B') to MegaSpanStyle.TextColorStyle(
                    SpanStyle(fontWeight = FontWeight.Medium),
                    textColor = TextColor.Primary,
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NAME_COLLISION_ALREADY_EXISTS_TAG),
        )

        MegaText(
            text = stringResource(
                id = when {
                    isFile -> sharedR.string.choose_file
                    collisionType == NameCollisionType.UPLOAD ->
                        sharedR.string.name_collision_choose_folder_header

                    else -> sharedR.string.choose_folder
                }
            ),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .testTag(NAME_COLLISION_SELECT_HEADER_TAG),
        )

        SubtleDivider(modifier = Modifier.padding(top = 16.dp))

        if (fileVersioningInfo != null && fileVersioningInfo.canReplaceOrUpdate) {
            val replaceInfo = replaceUpdateMergeInfo(collisionType, isFile, fileVersioningInfo.isFileVersioningEnabled)
            val replaceButton = replaceUpdateMergeButton(collisionType, isFile, fileVersioningInfo.isFileVersioningEnabled)

            MegaText(
                text = stringResource(replaceInfo),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag(NAME_COLLISION_REPLACE_INFO_TAG),
            )

            if (fileVersioningInfo.isFileVersioningEnabled && collisionType == NameCollisionType.UPLOAD) {
                MegaText(
                    text = stringResource(sharedR.string.general_learn_more),
                    linkColor = LinkColor.Primary,
                    style = AppTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(onClick = onLearnMore)
                        .testTag(NAME_COLLISION_LEARN_MORE_TAG),
                )
            }

            NameCollisionOptionCard(
                thumbnailData = collisionResult.thumbnail?.let { ThumbnailUriRequest(it) },
                defaultIcon = defaultIcon,
                name = name,
                info = if (isFile) {
                    formatFileSize(collision.size, context)
                } else {
                    folderInfo(collision.childFolderCount, collision.childFileCount)
                },
                date = formattedDate,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .testTag(NAME_COLLISION_REPLACE_CARD_TAG),
            )

            SecondaryFilledButton(
                text = stringResource(replaceButton),
                onClick = { onReplaceUpdateMerge(applyForAll) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag(NAME_COLLISION_REPLACE_BUTTON_TAG),
            )

            SubtleDivider(modifier = Modifier.padding(top = 14.dp))
        }

        val showRename = isFile || collisionType == NameCollisionType.UPLOAD
        if (showRename) {
            MegaText(
                text = stringResource(renameInfo(collisionType, isFile)),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag(NAME_COLLISION_RENAME_INFO_TAG),
            )

            NameCollisionOptionCard(
                thumbnailData = collisionResult.thumbnail
                    ?.takeIf { isFile }
                    ?.let { ThumbnailUriRequest(it) },
                defaultIcon = defaultIcon,
                name = collision.renameName.orEmpty(),
                info = null,
                date = null,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .testTag(NAME_COLLISION_RENAME_CARD_TAG),
            )

            SecondaryFilledButton(
                text = stringResource(renameButton(collisionType)),
                onClick = { onRename(applyForAll) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag(NAME_COLLISION_RENAME_BUTTON_TAG),
            )

            SubtleDivider(modifier = Modifier.padding(top = 14.dp))
        }

        MegaText(
            text = stringResource(
                if (isFile) sharedR.string.skip_file else sharedR.string.skip_folder
            ),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(NAME_COLLISION_CANCEL_INFO_TAG),
        )

        NameCollisionOptionCard(
            thumbnailData = collisionResult.collisionThumbnail?.let { ThumbnailUriRequest(it) },
            defaultIcon = defaultIcon,
            name = collisionResult.collisionName.orEmpty(),
            info = if (isFile) {
                formatFileSize(collisionResult.collisionSize ?: 0L, context)
            } else {
                collisionResult.collisionFolderContent
                    ?.let { folderInfo(it.numberOfFolders, it.numberOfFiles) }
            },
            date = formattedDate,
            modifier = Modifier
                .padding(top = 10.dp)
                .testTag(NAME_COLLISION_CANCEL_CARD_TAG),
        )

        SecondaryFilledButton(
            text = stringResource(cancelButton(collisionType)),
            onClick = { onCancel(applyForAll) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(NAME_COLLISION_CANCEL_BUTTON_TAG),
        )

        if (pendingCollisions > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .testTag(NAME_COLLISION_APPLY_FOR_ALL_TAG),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = applyForAll,
                    onCheckStateChanged = { applyForAll = it },
                    modifier = Modifier.testTag(NAME_COLLISION_APPLY_FOR_ALL_CHECKBOX_TAG),
                )
                MegaText(
                    text = pluralStringResource(
                        id = sharedR.plurals.general_name_collision_file_apply_for_all,
                        count = pendingCollisions,
                        pendingCollisions,
                    ),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NameCollisionOptionCard(
    thumbnailData: ThumbnailData?,
    @DrawableRes defaultIcon: Int,
    name: String,
    info: String?,
    date: String?,
    modifier: Modifier = Modifier,
) {
    RoundCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NodeThumbnailView(
                data = thumbnailData,
                contentDescription = null,
                defaultImage = defaultIcon,
                modifier = Modifier.size(48.dp),
                layoutType = ThumbnailLayoutType.List,
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            ) {
                MegaText(
                    text = name,
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                )
                info?.let {
                    MegaText(
                        text = it,
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                date?.let {
                    MegaText(
                        text = it,
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun folderInfo(numFolders: Int, numFiles: Int): String = when {
    numFolders == 0 && numFiles == 0 ->
        stringResource(sharedR.string.empty_file_browser_folder)

    numFolders == 0 ->
        pluralStringResource(sharedR.plurals.num_of_files_with_parameter, numFiles, numFiles)

    numFiles == 0 ->
        pluralStringResource(sharedR.plurals.num_of_folders_with_parameter, numFolders, numFolders)

    else -> pluralStringResource(
        sharedR.plurals.num_of_folders_and_num_of_files, numFolders, numFolders
    ) + pluralStringResource(
        sharedR.plurals.num_of_files_with_parameter, numFiles, numFiles
    )
}

private fun NameCollision.toNameCollisionType(): NameCollisionType = when (this) {
    is FileNameCollision -> NameCollisionType.UPLOAD
    is NodeNameCollision.Default ->
        if (type == NodeNameCollisionType.MOVE) NameCollisionType.MOVE else NameCollisionType.COPY

    is NodeNameCollision.Chat, is NodeNameCollision.FileLink -> NameCollisionType.COPY
}

private fun replaceUpdateMergeInfo(
    collisionType: NameCollisionType,
    isFile: Boolean,
    isFileVersioningEnabled: Boolean,
): Int = when (collisionType) {
    NameCollisionType.UPLOAD -> when {
        !isFile -> sharedR.string.warning_upload_and_merge
        isFileVersioningEnabled -> sharedR.string.warning_versioning_upload_and_update
        else -> sharedR.string.warning_upload_and_replace
    }

    NameCollisionType.COPY ->
        if (isFile) sharedR.string.warning_copy_and_replace else sharedR.string.warning_copy_and_merge

    NameCollisionType.MOVE ->
        if (isFile) sharedR.string.warning_move_and_replace else sharedR.string.warning_move_and_merge
}

private fun replaceUpdateMergeButton(
    collisionType: NameCollisionType,
    isFile: Boolean,
    isFileVersioningEnabled: Boolean,
): Int = when (collisionType) {
    NameCollisionType.UPLOAD -> when {
        !isFile -> sharedR.string.upload_and_merge
        isFileVersioningEnabled -> sharedR.string.upload_and_update
        else -> sharedR.string.upload_and_replace
    }

    NameCollisionType.COPY ->
        if (isFile) sharedR.string.copy_and_replace else sharedR.string.copy_and_merge

    NameCollisionType.MOVE ->
        if (isFile) sharedR.string.move_and_replace else sharedR.string.move_and_merge
}

private fun renameInfo(collisionType: NameCollisionType, isFile: Boolean): Int =
    when (collisionType) {
        NameCollisionType.UPLOAD ->
            if (isFile) {
                sharedR.string.warning_upload_and_rename
            } else {
                sharedR.string.name_collision_folder_warning_upload_and_rename
            }

        NameCollisionType.COPY -> sharedR.string.warning_copy_and_rename
        NameCollisionType.MOVE -> sharedR.string.warning_move_and_rename
    }

private fun renameButton(collisionType: NameCollisionType): Int = when (collisionType) {
    NameCollisionType.UPLOAD -> sharedR.string.upload_and_rename
    NameCollisionType.COPY -> sharedR.string.copy_and_rename
    NameCollisionType.MOVE -> sharedR.string.move_and_rename
}

private fun cancelButton(collisionType: NameCollisionType): Int = when (collisionType) {
    NameCollisionType.UPLOAD -> sharedR.string.do_not_upload
    NameCollisionType.COPY -> sharedR.string.do_not_copy
    NameCollisionType.MOVE -> sharedR.string.do_not_move
}

internal const val NAME_COLLISION_SCREEN_TAG = "name_collision_screen:screen"
internal const val NAME_COLLISION_LOADING_TAG = "name_collision_screen:loading"
internal const val NAME_COLLISION_ALREADY_EXISTS_TAG = "name_collision_screen:already_exists"
internal const val NAME_COLLISION_SELECT_HEADER_TAG = "name_collision_screen:select_header"
internal const val NAME_COLLISION_REPLACE_INFO_TAG = "name_collision_screen:replace_info"
internal const val NAME_COLLISION_LEARN_MORE_TAG = "name_collision_screen:learn_more"
internal const val NAME_COLLISION_REPLACE_CARD_TAG = "name_collision_screen:replace_card"
internal const val NAME_COLLISION_REPLACE_BUTTON_TAG = "name_collision_screen:replace_button"
internal const val NAME_COLLISION_RENAME_INFO_TAG = "name_collision_screen:rename_info"
internal const val NAME_COLLISION_RENAME_CARD_TAG = "name_collision_screen:rename_card"
internal const val NAME_COLLISION_RENAME_BUTTON_TAG = "name_collision_screen:rename_button"
internal const val NAME_COLLISION_CANCEL_INFO_TAG = "name_collision_screen:cancel_info"
internal const val NAME_COLLISION_CANCEL_CARD_TAG = "name_collision_screen:cancel_card"
internal const val NAME_COLLISION_CANCEL_BUTTON_TAG = "name_collision_screen:cancel_button"
internal const val NAME_COLLISION_APPLY_FOR_ALL_TAG = "name_collision_screen:apply_for_all"
internal const val NAME_COLLISION_APPLY_FOR_ALL_CHECKBOX_TAG =
    "name_collision_screen:apply_for_all_checkbox"

@CombinedThemePreviews
@Composable
private fun NameCollisionScreenPreview() {
    AndroidThemeForPreviews {
        NameCollisionScreen(
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
                pendingFileCollisions = 0,
                pendingFolderCollisions = 0,
                currentCollisionIndex = 1,
                currentTypeCollisionCount = 1,
                hasPendingCollisions = false,
                uploadedFilesCount = 0,
                uploadEvent = consumed(),
                outcome = consumed(),
                dismissEvent = consumed,
            ),
            onReplaceUpdateMerge = {},
            onCancel = {},
            onRename = {},
            onLearnMore = {},
            onBack = {},
        )
    }
}
