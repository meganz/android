package mega.privacy.android.feature.fileinfo.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState

/**
 * The loading skeleton for the File Info screen: a shimmering header placeholder followed by a few
 * placeholder detail rows. Laid out through [FileInfoResponsiveLayout] so it adapts to portrait,
 * landscape, and tablet exactly like the loaded content and swaps in without a reflow.
 *
 * @param modifier modifier for the skeleton
 */
@Composable
internal fun FileInfoLoading(modifier: Modifier = Modifier) {
    FileInfoResponsiveLayout(
        modifier = modifier.testTag(FILE_INFO_LOADING_TAG),
        header = { headerModifier ->
            Box(headerModifier.shimmerEffect(shape = RoundedCornerShape(8.dp)))
        },
        details = {
            RowDetailsSkeletonItem()
            RowDetailsSkeletonItem()
            RowDetailsSkeletonItem()
        },
    )
}

@Composable
private fun RowDetailsSkeletonItem() {
    Column {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(20.dp)
                .shimmerEffect(shape = RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .shimmerEffect(shape = RoundedCornerShape(4.dp)),
        )
    }
}

internal const val FILE_INFO_LOADING_TAG = "file_info_screen:loading"

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileInfoScreenLoadingPreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(isLoading = true),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
        )
    }
}
