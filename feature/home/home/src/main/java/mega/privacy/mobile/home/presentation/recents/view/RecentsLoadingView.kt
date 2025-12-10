package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme

@Composable
internal fun RecentsLoadingView(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(RECENTS_LOADING_TEST_TAG)
    ) {
        Box(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            MegaText(
                "",
                style = AppTheme.typography.labelMedium,
            )

            Spacer(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
                    .shimmerEffect()
            )
        }

        RecentsListItemViewSkeleton()
        RecentsListItemViewSkeleton()
        RecentsListItemViewSkeleton()
        RecentsListItemViewSkeleton()
    }
}

/**
 * List skeleton item that exactly matches NodeListViewItem layout.
 * Uses exact spacing: horizontal = DSTokens.spacings.s4 (16.dp), vertical = DSTokens.spacings.s3 (12.dp)
 */
@Composable
fun RecentsListItemViewSkeleton() {
    GenericListItem(
        verticalAlignment = Alignment.Top,
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp
        ),
        leadingElement = {
            Spacer(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(32.dp)
                    .shimmerEffect(RoundedCornerShape(6.dp))
            )
        },
        title = {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = " ",
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                    style = AppTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                )
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.6f)
                        .shimmerEffect(),
                )
            }
        },
        subtitle = {
            Spacer(Modifier.height(4.dp))
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = " ",
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                )
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.4f)
                        .shimmerEffect(),
                )
            }
        }
    )
}

internal const val RECENTS_LOADING_TEST_TAG = "recents_widget:loading"


@CombinedThemePreviews
@Composable
private fun RecentsLoadingViewPreview() {
    AndroidThemeForPreviews {
        RecentsLoadingView()
    }
}

