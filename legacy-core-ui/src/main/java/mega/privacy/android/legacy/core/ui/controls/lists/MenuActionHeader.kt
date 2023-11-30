package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * MenuHeaderListViewItem
 *
 * One line menu title as per figma designs
 * @param text header title
 * @param modifier
 */
@Composable
fun MenuActionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            modifier = Modifier.testTag(MENU_HEADER_TEXT_TAG),
            text = text,
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.textColorSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal const val MENU_HEADER_TEXT_TAG = "menu_list_view_header_item:text_title"

@CombinedThemePreviews
@Composable
private fun PreviewMenuActionHeader() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MenuActionHeader(
            text = "Menu item label test very big item check ellipsis on text",
        )
    }
}