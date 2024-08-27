package mega.privacy.android.app.presentation.upload

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Upload View
 */
@Composable
fun UploadDestinationView(
    confirmImport: () -> Unit,
    consumeNameValidationError: () -> Unit,
    editFileName: (ImportUiItem?) -> Unit,
    updateFileName: (String) -> Unit,
    uiState: UploadDestinationUiState,
) {
    val scaffoldState = rememberScaffoldState()
    MegaScaffold(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                title = stringResource(id = R.string.title_upload_explorer),
                appBarType = AppBarType.BACK_NAVIGATION,
                elevation = 0.dp,
            )
        },
    ) {
        val showMore = rememberSaveable {
            mutableStateOf(false)
        }
        val itemsToShow =
            if (showMore.value) uiState.importUiItems else uiState.importUiItems.take(4)

        EventEffect(
            event = uiState.navigateToUpload,
            onConsumed = { },
        ) { items ->
            scaffoldState.snackbarHostState.showSnackbar("Navigate to upload screen with ${items.size} items")
        }

        EventEffect(
            event = uiState.nameValidationError,
            onConsumed = { consumeNameValidationError() }) {
            scaffoldState.snackbarHostState.showSnackbar(it)
        }

        LazyColumn(
            modifier = Modifier.testTag(UPLOAD_DESTINATION_VIEW_FILE_LIST_VIEW),
        ) {
            item {
                MegaText(
                    text = if (uiState.importUiItems.firstOrNull()?.isUrl == true) {
                        stringResource(id = R.string.file_properties_shared_folder_public_link_name)
                    } else {
                        pluralStringResource(
                            id = R.plurals.general_num_files,
                            count = uiState.importUiItems.size
                        )
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag(UPLOAD_DESTINATION_VIEW_HEADER_TEXT),
                    textColor = TextColor.Secondary,
                )
            }
            items(itemsToShow.size) { item ->
                UploadDestinationRowItem(
                    importUiItem = uiState.importUiItems[item],
                    isEditMode = uiState.editableFile == uiState.importUiItems[item]
                            || uiState.importUiItems[item].error.isNullOrBlank().not(),
                    editFileName = editFileName,
                    updateFileName = updateFileName,
                )
                if (item < itemsToShow.size - 1) {
                    MegaDivider(dividerType = DividerType.FullSize)
                }
            }
            item {
                UploadDestinationFooterView(
                    fileList = uiState.importUiItems,
                    showMore = showMore,
                    confirmImport = confirmImport
                )
            }
        }
    }
}

@Composable
private fun UploadDestinationFooterView(
    fileList: List<ImportUiItem>,
    showMore: MutableState<Boolean>,
    confirmImport: () -> Unit,
) {
    if (fileList.size > 4) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable {
                    showMore.value = !showMore.value
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            MegaText(
                text = stringResource(
                    id = if (showMore.value) {
                        R.string.general_show_less
                    } else {
                        R.string.general_show_more
                    }
                ),
                textColor = TextColor.Accent,
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .weight(1f)
                    .testTag(UPLOAD_DESTINATION_VIEW_SHOW_MORE_TEXT),
            )
            Icon(
                painter = painterResource(
                    id = if (showMore.value) {
                        R.drawable.ic_expand
                    } else {
                        R.drawable.ic_collapse_acc
                    }
                ),
                modifier = Modifier
                    .size(24.dp)
                    .testTag(UPLOAD_DESTINATION_VIEW_SHOW_MORE_ICON),
                tint = MaterialTheme.colors.secondary,
                contentDescription = ""
            )
        }
        MegaDivider(
            dividerType = DividerType.SmallStartPadding,
            modifier = Modifier.testTag(UPLOAD_DESTINATION_VIEW_SHOW_MORE_DIVIDER),
        )
    }
    MegaText(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(UPLOAD_DESTINATION_VIEW_CHOOSE_DESTINATION_TEXT),
        text = stringResource(id = R.string.choose_destionation),
        textColor = TextColor.Secondary,
        style = MaterialTheme.typography.body2,
    )

    TextMegaButton(
        modifier = Modifier.testTag(UPLOAD_DESTINATION_VIEW_CLOUD_DRIVE),
        text = stringResource(id = R.string.section_cloud_drive),
        onClick = {
            confirmImport()
            //navigate to cloud drive
        },
        textAlign = TextAlign.Start,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    )

    TextMegaButton(
        modifier = Modifier.testTag(UPLOAD_DESTINATION_VIEW_CHAT),
        text = stringResource(id = R.string.section_chat),
        onClick = {
            confirmImport()
            //navigate to chat
        },
        textAlign = TextAlign.Start,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    )
}

@CombinedThemePreviews
@Composable
private fun UploadViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        UploadDestinationView(
            uiState = UploadDestinationUiState(
                importUiItems = listOf(
                    ImportUiItem(fileName = "file1", filePath = "path1"),
                    ImportUiItem(fileName = "file2", filePath = "path2"),
                    ImportUiItem(fileName = "file3", filePath = "path3"),
                    ImportUiItem(fileName = "file4", filePath = "path4"),
                ),
            ),
            confirmImport = {},
            consumeNameValidationError = {},
            editFileName = {},
            updateFileName = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun UploadViewInEditModePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        UploadDestinationView(
            uiState = UploadDestinationUiState(
                importUiItems = listOf(
                    ImportUiItem(fileName = "file1", filePath = "path1"),
                    ImportUiItem(fileName = "file2", filePath = "path2"),
                    ImportUiItem(fileName = "file3", filePath = "path3"),
                    ImportUiItem(fileName = "file4", filePath = "path4"),
                    ImportUiItem(fileName = "file5", filePath = "path5"),
                    ImportUiItem(fileName = "file6", filePath = "path6"),
                    ImportUiItem(fileName = "file7", filePath = "path7"),
                    ImportUiItem(fileName = "file8", filePath = "path8"),
                    ImportUiItem(fileName = "file9", filePath = "path9"),
                ),
                editableFile = ImportUiItem(fileName = "file5", filePath = "path5"),
            ),
            confirmImport = {},
            consumeNameValidationError = {},
            editFileName = {},
            updateFileName = {},
        )
    }
}

internal const val UPLOAD_DESTINATION_VIEW_FILE_LIST_VIEW =
    "upload_destination_view:files_list_view"
internal const val UPLOAD_DESTINATION_VIEW_CLOUD_DRIVE = "upload_destination_view:cloud_drive"
internal const val UPLOAD_DESTINATION_VIEW_CHAT = "upload_destination_view:chat"
internal const val UPLOAD_DESTINATION_VIEW_SHOW_MORE_ICON = "upload_destination_view:showMoreIcon"
internal const val UPLOAD_DESTINATION_VIEW_SHOW_MORE_TEXT = "upload_destination_view:show_more_text"
internal const val UPLOAD_DESTINATION_VIEW_SHOW_MORE_DIVIDER =
    "upload_destination_view:show_more_divider"
internal const val UPLOAD_DESTINATION_VIEW_CHOOSE_DESTINATION_TEXT =
    "upload_destination_view:choose_destination_text"
internal const val UPLOAD_DESTINATION_VIEW_HEADER_TEXT = "upload_destination_view:header_text"

