package mega.privacy.android.shared.search.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.search.presentation.model.SearchEmptyContent


@Composable
fun SearchEmptyStateView(
    content: SearchEmptyContent,
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
        title = content.title.text,
        description = SpannableText(text = content.description.text),
        imagePainter = painterResource(id = content.image),
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchEmptyStateView() {
    AndroidThemeForPreviews {
        SearchEmptyStateView(
            content = SearchEmptyContent(
                title = LocalizedText.Literal("Nothing here"),
                description = LocalizedText.Literal("Try a different search"),
                image = mega.privacy.android.icon.pack.R.drawable.ic_search_02,
            )
        )
    }
}
