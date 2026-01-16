package mega.privacy.android.feature.clouddrive.presentation.search.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR


@Composable
fun SearchEmptyView(
    modifier: Modifier = Modifier,
) {
    val isLandscapeMode =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    EmptyStateView(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .conditional(!isLandscapeMode) {
                imePadding()
            },
        title = stringResource(id = sharedR.string.photos_search_empty_state_title),
        description = SpannableText(text = stringResource(id = sharedR.string.photos_search_empty_state_description)),
        illustration = IconPackR.drawable.ic_search_02
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchEmptyView() {
    AndroidThemeForPreviews {
        SearchEmptyView()
    }
}