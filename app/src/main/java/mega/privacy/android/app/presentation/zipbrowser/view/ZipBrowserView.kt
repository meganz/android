package mega.privacy.android.app.presentation.zipbrowser.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ZipBrowserView(
    items: List<ZipInfoUiEntity>,
    parentFolderName: String,
    folderDepth: Int,
    modifier: Modifier = Modifier,
    onItemClicked: (ZipInfoUiEntity) -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val lazyListState = rememberLazyListState()

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
        }
    ) { paddingValues ->
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

@CombinedThemePreviews
@Composable
private fun ZipBrowserViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ZipBrowserView(
            items = initPreviewData(),
            parentFolderName = "Folder name",
            folderDepth = 0
        )
    }
}

private fun initPreviewData(): List<ZipInfoUiEntity> =
    (0..6).map {
        if (it == 3) {
            initZipInfoUiEntity(
                icon = R.drawable.ic_compressed_medium_solid,
                name = "zip file",
                index = it,
                info = "100 KB",
                type = ZipEntryType.Zip
            )
        } else if (it < 3) {
            initZipInfoUiEntity(
                icon = R.drawable.ic_folder_medium_solid,
                name = "zip folder",
                index = it,
                info = "empty",
                type = ZipEntryType.Folder
            )
        } else {
            initZipInfoUiEntity(
                icon = R.drawable.ic_text_medium_solid,
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