package mega.privacy.android.app.presentation.offline.view


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.shared.original.core.ui.controls.skeleton.ListItemLoadingSkeleton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * Loading state view for offline page
 * @param modifier [Modifier]
 */
@Composable
fun OfflineLoadingView(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag(OFFLINE_LOADING_VIEW_TEST_TAG),
    ) {
        items(count = 10) {
            ListItemLoadingSkeleton()
        }
    }
}

internal const val OFFLINE_LOADING_VIEW_TEST_TAG = "offline_loading_view_test_tag"

@CombinedThemePreviews
@Composable
private fun OfflineLoadingViewListPreview() {
    OfflineLoadingView()
}