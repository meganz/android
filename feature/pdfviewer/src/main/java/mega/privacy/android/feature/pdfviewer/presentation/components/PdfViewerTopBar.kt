package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.navigation.contract.menu.CommonMenuAction

/**
 * Top app bar for the PDF Viewer. Overlays the page; toggled by tapping the document.
 *
 * @param title The title to display (file name)
 * @param onBack Callback for back navigation
 * @param onSearch Callback when the search icon is tapped
 * @param onOpenNodeOptions Callback for opening node options (More button)
 * @param showMoreAction Whether to show the More (node options) button. False for external files.
 * @param modifier Modifier for the composable
 */
@Composable
internal fun PdfViewerTopBar(
    title: String?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenNodeOptions: () -> Unit,
    modifier: Modifier = Modifier,
    showMoreAction: Boolean = true,
) {
    MegaTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = title.orEmpty(),
        navigationType = AppBarNavigationType.Back(onBack),
        actions = buildList {
            add(MenuActionWithClick(CommonMenuAction.Search) { onSearch() })
            if (showMoreAction) {
                add(MenuActionWithClick(CommonMenuAction.More) { onOpenNodeOptions() })
            }
        },
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
