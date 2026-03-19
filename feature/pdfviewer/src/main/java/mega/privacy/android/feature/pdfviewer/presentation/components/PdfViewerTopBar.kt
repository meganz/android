package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction

/**
 * Top app bar for the PDF Viewer.
 *
 * Always visible at the top of the screen with the PDF content below it.
 * The [onSearch] icon triggers search mode.
 *
 * @param title The title to display (file name)
 * @param onBack Callback for back navigation
 * @param onSearch Callback when the search icon is tapped
 * @param onOpenNodeOptions Callback for opening node options (More button)
 * @param modifier Modifier for the composable
 */
@Composable
internal fun PdfViewerTopBar(
    title: String?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenNodeOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = title.orEmpty(),
        navigationType = AppBarNavigationType.Back(onBack),
        actions = listOf(
            MenuActionWithClick(CommonAppBarAction.Search) { onSearch() },
            MenuActionWithClick(CommonAppBarAction.More) { onOpenNodeOptions() },
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewPdfViewerTopBar() {
    AndroidThemeForPreviews {
        PdfViewerTopBar(
            title = "Document.pdf",
            onBack = {},
            onSearch = {},
            onOpenNodeOptions = {}
        )
    }
}
