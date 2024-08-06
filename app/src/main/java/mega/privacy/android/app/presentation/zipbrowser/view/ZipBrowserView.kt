package mega.privacy.android.app.presentation.zipbrowser.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ZipBrowserView(
    items: List<ZipInfoUiEntity>,
    parentFolderName: String,
    folderDepth: Int,
    showProgressBar: Boolean,
    showAlertDialog: Boolean,
    showSnackBar: Boolean,
    modifier: Modifier = Modifier,
    onItemClicked: (ZipInfoUiEntity) -> Unit = {},
    onBackPressed: () -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onSnackBarShown: () -> Unit = {},
) {
    val lazyListState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showSnackBar) {
        if (showSnackBar) {
            coroutineScope.launch {
                snackBarHostState.showAutoDurationSnackbar(
                    context.resources.getString(R.string.message_zip_format_error)
                )
            }
            onSnackBarShown()
        }
    }

    BackHandler(folderDepth > 0) {
        onBackPressed()
    }

    MegaScaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = parentFolderName,
                modifier = Modifier.testTag(ZIP_BROWSER_TOP_BAR_TEST_TAG),
                onNavigationPressed = onBackPressed
            )
        },
        scaffoldState = rememberScaffoldState(snackbarHostState = snackBarHostState)
    ) { paddingValues ->
        if (showAlertDialog) {
            ZipBrowserAlertDialog(onDialogDismiss)
        }

        if (showProgressBar) {
            UnzipProgressBarView()
        }

        if (items.isNotEmpty()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(ZIP_BROWSER_LIST_TEST_TAG)
            ) {
                items(count = items.size, key = { items[it].path }) {
                    val zipItem = items[it]
                    NodeListViewItem(
                        title = zipItem.name,
                        subtitle = zipItem.info,
                        icon = zipItem.icon,
                        onItemClicked = { onItemClicked(zipItem) }
                    )
                    MegaDivider(
                        dividerType = DividerType.BigStartPadding,
                        modifier = Modifier.testTag("$ZIP_BROWSER_ITEM_DIVIDER_TEST_TAG$it")
                    )
                }
            }
        }
    }
}

@Composable
private fun ZipBrowserAlertDialog(
    onDialogDismiss: () -> Unit = {},
) {
    MegaAlertDialog(
        modifier = Modifier.testTag(ZIP_BROWSER_ALERT_DIALOG_TEST_TAG),
        text = stringResource(R.string.error_fail_to_open_file_general),
        confirmButtonText = stringResource(R.string.general_ok),
        cancelButtonText = null,
        onConfirm = onDialogDismiss,
        onDismiss = onDialogDismiss
    )
}

@Composable
private fun UnzipProgressBarView(
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = {}
    ) {
        Row(
            modifier = modifier
                .background(colorResource(id = R.color.white_dark_grey))
                .padding(10.dp)
                .testTag(ZIP_BROWSER_UNZIP_PROGRESS_BAR_TEST_TAG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MegaCircularProgressIndicator(
                modifier = Modifier.padding(start = 10.dp),
                strokeWidth = 4.dp,
            )

            MegaText(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = stringResource(
                    id = R.string.unzipping_process
                ),
                textColor = TextColor.Primary
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ZipBrowserViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ZipBrowserView(
            items = initPreviewData(),
            parentFolderName = "Folder name",
            folderDepth = 0,
            showProgressBar = false,
            showAlertDialog = false,
            showSnackBar = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ZipBrowserViewWithProgressBarPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ZipBrowserView(
            items = initPreviewData(),
            parentFolderName = "Folder name",
            folderDepth = 0,
            showProgressBar = false,
            showAlertDialog = true,
            showSnackBar = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ZipBrowserViewWithAlertDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ZipBrowserView(
            items = initPreviewData(),
            parentFolderName = "Folder name",
            folderDepth = 0,
            showProgressBar = true,
            showAlertDialog = false,
            showSnackBar = false
        )
    }
}

private fun initPreviewData(): List<ZipInfoUiEntity> =
    (0..6).map {
        if (it == 3) {
            initZipInfoUiEntity(
                icon = iconPackR.drawable.ic_compressed_medium_solid,
                name = "zip file",
                index = it,
                info = "100 KB",
                type = ZipEntryType.Zip
            )
        } else if (it < 3) {
            initZipInfoUiEntity(
                icon = iconPackR.drawable.ic_folder_medium_solid,
                name = "zip folder",
                index = it,
                info = "empty",
                type = ZipEntryType.Folder
            )
        } else {
            initZipInfoUiEntity(
                icon = iconPackR.drawable.ic_text_medium_solid,
                name = "file",
                index = it,
                info = "200 MB",
                type = ZipEntryType.File
            )
        }
    }

private fun initZipInfoUiEntity(
    icon: Int,
    name: String,
    index: Int,
    info: String,
    type: ZipEntryType,
) =
    ZipInfoUiEntity(
        icon = icon,
        name = name,
        path = "Zip path $index",
        info = info,
        zipEntryType = type
    )

@CombinedThemePreviews
@Composable
private fun UnzipProgressBarViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        UnzipProgressBarView()
    }
}

/**
 * Test tag for the top bar of the zip browser
 */
const val ZIP_BROWSER_TOP_BAR_TEST_TAG = "zip_browser:top_bar"

/**
 * Test tag for the list of the zip browser
 */
const val ZIP_BROWSER_LIST_TEST_TAG = "zip_browser:lazy_colum_list"

/**
 * Test tag for the divider of zip browser item
 */
const val ZIP_BROWSER_ITEM_DIVIDER_TEST_TAG = "zip_browser_item:divider"

/**
 * Test tag for the unzip progress bar
 */
const val ZIP_BROWSER_UNZIP_PROGRESS_BAR_TEST_TAG = "zip_browser:progress_bar_unzip"

/**
 * Test tag for the alert dialog
 */
const val ZIP_BROWSER_ALERT_DIALOG_TEST_TAG = "zip_browser:dialog_alert"