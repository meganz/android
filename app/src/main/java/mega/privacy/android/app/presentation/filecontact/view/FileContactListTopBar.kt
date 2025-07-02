package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.filecontact.model.SelectionState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

@Composable
internal fun FileContactListTopBar(
    folderName: String,
    selectionState: SelectionState,
    onBackPressed: () -> Unit,
    selectAll: () -> Unit,
    deselectAll: () -> Unit,
    changePermissions: () -> Unit,
    shareFolder: () -> Unit,
    removeShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectionState.selectedCount > 0) {
        MegaTopAppBar(
            modifier = modifier.testTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR),
            title = pluralStringResource(
                R.plurals.general_selection_num_contacts,
                selectionState.selectedCount,
                selectionState.selectedCount
            ),
            navigationType = AppBarNavigationType.Close {
                deselectAll()
            },
            trailingIcons = {
                SelectModeActions(
                    selectionState = selectionState,
                    changePermissions = changePermissions,
                    removeShare = removeShare,
                    selectAll = selectAll,
                    deselectAll = deselectAll,
                )
            }
        )
    } else {
        MegaTopAppBar(
            modifier = modifier.testTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR),
            title = folderName,
            navigationType = AppBarNavigationType.Back(onBackPressed),
            trailingIcons = {
                DefaultActions(
                    shareFolder = shareFolder,
                    selectAll = selectAll,
                )
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewFileContactListTopBar(
    @PreviewParameter(SelectionStatePreviewParameterProvider::class) selectionState: SelectionState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Scaffold(
            topBar = {
                FileContactListTopBar(
                    folderName = "Richard Daguerreotypes",
                    selectionState = selectionState,
                    onBackPressed = {},
                    selectAll = {},
                    deselectAll = {},
                    changePermissions = {},
                    shareFolder = {},
                    removeShare = {},
                    modifier = Modifier.systemBarsPadding(),
                )
            }
        ) {
            Text(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .wrapContentSize(align = Alignment.Center),
                text = "Content",
            )
        }
    }
}

@Composable
private fun RowScope.DefaultActions(
    shareFolder: () -> Unit,
    selectAll: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
        modifier = Modifier.testTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SHARE_FOLDER_ITEM),
        onClick = shareFolder,
    ) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Regular.Outline.Folder),
            contentDescription = stringResource(R.string.context_share_folder),
            tint = IconColor.Primary,
        )
    }

    IconButton(
        modifier = Modifier.testTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW),
        onClick = { expanded = true },
    ) {
        MegaIcon(
            rememberVectorPainter(IconPack.Medium.Regular.Outline.MoreVertical),
            contentDescription = "More items Icon",
            tint = IconColor.Primary,
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            modifier = Modifier.testTag(
                TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM
            ),
            text = { Text(stringResource(R.string.action_select_all)) },
            onClick = {
                expanded = false
                selectAll()
            }
        )
    }
}

@Composable
private fun RowScope.SelectModeActions(
    selectionState: SelectionState,
    changePermissions: () -> Unit,
    removeShare: () -> Unit,
    selectAll: () -> Unit,
    deselectAll: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        modifier = Modifier.testTag(
            TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_CHANGE_PERMISSION_ITEM
        ),
        onClick = changePermissions,
    ) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Regular.Outline.Key02),
            contentDescription = stringResource(R.string.file_properties_shared_folder_change_permissions),
            tint = IconColor.Primary,
        )
    }

    IconButton(
        modifier = Modifier.testTag(
            TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_REMOVE_SHARE
        ),
        onClick = removeShare,
    ) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Regular.Outline.X),
            contentDescription = stringResource(R.string.context_remove),
            tint = IconColor.Primary,
        )
    }

    IconButton(
        modifier = Modifier.testTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW),
        onClick = { expanded = true },
    ) {
        MegaIcon(
            rememberVectorPainter(IconPack.Medium.Regular.Outline.MoreVertical),
            contentDescription = "More items Icon",
            tint = IconColor.Primary,
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        if (selectionState.allSelected.not()) {
            DropdownMenuItem(
                modifier = Modifier.testTag(
                    TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM
                ),
                text = { Text(stringResource(R.string.action_select_all)) },
                onClick = {
                    expanded = false
                    selectAll()
                }
            )
        }
        DropdownMenuItem(
            modifier = Modifier.testTag(
                TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM
            ),
            text = { Text(stringResource(R.string.action_unselect_all)) },
            onClick = {
                expanded = false
                deselectAll()
            }
        )
    }
}

private class SelectionStatePreviewParameterProvider : PreviewParameterProvider<SelectionState> {
    override val values: Sequence<SelectionState>
        get() = sequenceOf(
            SelectionState(0, false),
            SelectionState(1, false),
            SelectionState(2, true),
        )
}

internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR = "file_contact_list_top_bar"
internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SHARE_FOLDER_ITEM =
    "file_contact_list_top_bar:menu_item_share_folder"
internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_REMOVE_SHARE =
    "file_contact_list_top_bar:menu_item_remove_share"
internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_CHANGE_PERMISSION_ITEM =
    "file_contact_list_top_bar:menu_item_change_permissions"
internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM =
    "file_contact_list_top_bar:menu_item_select_all"
internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM =
    "file_contact_list_top_bar:menu_item_unselect_all"
internal const val TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW =
    "file_contact_list_top_bar:menu_item_overflow"