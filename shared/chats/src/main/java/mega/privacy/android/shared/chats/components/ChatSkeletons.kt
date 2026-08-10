package mega.privacy.android.shared.chats.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.extensions.delayedTrue
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import kotlin.time.Duration

/**
 * A skeleton view that mimics the chat explorer list layout with shimmer effects.
 * Shows a section header placeholder followed by placeholder chat rows while loading.
 * @param delay Delay before showing the skeleton. Default is 0 that means it will show immediately.
 */
@Composable
fun ChatsViewSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(top = 8.dp),
    delay: Duration = Duration.ZERO,
) {
    val shouldShowSkeleton by delayedTrue(delay)
    if (shouldShowSkeleton) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            userScrollEnabled = false,
        ) {
            item { ChatSectionHeaderSkeleton() }
            items(20) { ChatListItemSkeleton() }
        }
    }
}

/**
 * Section header skeleton that matches the chat explorer section headers.
 */
@Composable
private fun ChatSectionHeaderSkeleton() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .width(120.dp)
            .height(16.dp)
            .shimmerEffect(),
    )
}

/**
 * List skeleton item that matches the chat explorer row layout.
 */
@Composable
fun ChatListItemSkeleton() {
    GenericListItem(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        leadingElement = {
            Spacer(
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .shimmerEffect(CircleShape),
            )
        },
        title = {
            Spacer(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .fillMaxWidth(0.5f)
                    .height(18.dp)
                    .shimmerEffect(),
            )
        },
        subtitle = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(14.dp)
                    .shimmerEffect(),
            )
        },
        trailingElement = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Spacer(
                    modifier = Modifier
                        .size(20.dp)
                        .shimmerEffect(RoundedCornerShape(6.dp)),
                )
            }
        },
    )
}

@CombinedThemePreviews
@Composable
private fun ChatsViewSkeletonPreview() {
    AndroidThemeForPreviews {
        ChatsViewSkeleton()
    }
}
