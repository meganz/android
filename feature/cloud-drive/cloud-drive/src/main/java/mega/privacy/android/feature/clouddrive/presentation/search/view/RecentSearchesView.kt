package mega.privacy.android.feature.clouddrive.presentation.search.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * View to display recent searches
 *
 * @param queries List of recent search strings
 * @param onClicked Callback when a recent search is clicked, with the search string and a boolean for keyboard
 * @param onClearAllClicked Callback when the "Clear All" button is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun RecentSearchesView(
    queries: List<String>,
    onClicked: (String, Boolean) -> Unit,
    onClearAllClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MegaText(
                text = stringResource(sharedR.string.photos_search_recent_search),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
            )
            TextButton(onClick = onClearAllClicked) {
                MegaText(
                    text = stringResource(sharedR.string.general_clear_all),
                    textColor = TextColor.Error,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.W400
                    ),
                )
            }
        }
        LazyColumn {
            items(
                items = queries,
                key = { it }
            ) { text ->
                FlexibleLineListItem(
                    title = text,
                    leadingElement = {
                        MegaIcon(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchSmall),
                            contentDescription = null,
                            tint = IconColor.Secondary
                        )
                    },
                    trailingElement = {
                        MegaIcon(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    onClicked(text, true) // Open with keyboard
                                },
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ArrowUpLeft),
                            contentDescription = null,
                            tint = IconColor.Secondary
                        )
                    },
                    onClickListener = {
                        onClicked(text, false)
                    }
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RecentSearchesViewPreview() {
    AndroidThemeForPreviews {
        RecentSearchesView(
            queries = listOf("my video folder", "document", "photo"),
            onClicked = { _, _ -> },
            onClearAllClicked = {},
        )
    }
}

