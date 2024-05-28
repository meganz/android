package mega.privacy.android.shared.original.core.ui.controls.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.pager.indicator.HorizontalPagerIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A [HorizontalPager] that supports infinite scrolling with indicator.
 *
 * @param pageCount The total of the actual items.
 * @param modifier A modifier instance to be applied to this Pager outer layout.
 * @param isOverScrollModeEnable Whether the over scroll configuration is allowed.
 * @param beyondBoundsPageCount Pages to compose and layout before and after the list of visible
 * pages.
 * @param verticalAlignment How pages are aligned vertically in this Pager.
 * @param pageContent This Pager's page Composable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteHorizontalPagerWithIndicator(
    pageCount: Int,
    modifier: Modifier = Modifier,
    isOverScrollModeEnable: Boolean = true,
    beyondBoundsPageCount: Int = PagerDefaults.BeyondBoundsPageCount,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    pagerIndicator: @Composable ((currentPage: Int) -> Unit)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    // Set the raw page count to a large number for infinite scroll behavior.
    // Use a number that is divisible by two, e.g. 100.
    // So, that we can display the first item as the middle item which will be the initial page.
    val infinitePageCount = pageCount * 100
    val pagerState = rememberPagerState(
        // Starts from the middle item
        initialPage = infinitePageCount / 2,
        pageCount = { infinitePageCount }
    )
    // Set the overscroll animation configuration
    CompositionLocalProvider(
        value = LocalOverscrollConfiguration provides if (isOverScrollModeEnable) {
            OverscrollConfiguration()
        } else {
            null
        }
    ) {
        HorizontalPager(
            modifier = modifier,
            state = pagerState,
            beyondBoundsPageCount = beyondBoundsPageCount,
            verticalAlignment = verticalAlignment
        ) { page ->
            pageContent(page % pageCount)
        }
    }

    pagerIndicator?.invoke(pagerState.currentPage % pageCount) ?: HorizontalPagerIndicator(
        modifier = Modifier.fillMaxWidth(),
        pageSize = pageCount,
        currentPage = pagerState.currentPage % pageCount
    )
}

@OptIn(ExperimentalFoundationApi::class)
@CombinedTextAndThemePreviews
@Composable
private fun InfiniteHorizontalPagerWithIndicatorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Column(modifier = Modifier.fillMaxSize()) {
            InfiniteHorizontalPagerWithIndicator(
                modifier = Modifier.fillMaxWidth(),
                pageCount = 5,
                verticalAlignment = Alignment.Top
            ) { page ->
                MegaText(
                    modifier = Modifier.fillMaxWidth(),
                    text = page.toString(),
                    textColor = TextColor.Primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
