package mega.privacy.android.shared.original.core.ui.controls.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextAlign
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.original.core.ui.controls.pager.indicator.HORIZONTAL_PAGER_INDICATOR_TAG
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@RunWith(AndroidJUnit4::class)
class InfiniteHorizontalPagerWithIndicatorKtTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the provided pager indicator is shown when not NULL`() {
        val text = "indicator"
        with(composeRule) {
            setContent(
                pageCount = 1,
                pagerIndicator = {
                    Text(text = text)
                }
            )

            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the default pager indicator is shown when the custom pager indicator is not provided`() {
        with(composeRule) {
            setContent(pageCount = 1)

            onNodeWithTag(HORIZONTAL_PAGER_INDICATOR_TAG).assertIsDisplayed()
        }
    }

    private fun ComposeContentTestRule.setContent(
        pageCount: Int = 0,
        isOverScrollModeEnable: Boolean = true,
        beyondBoundsPageCount: Int = PagerDefaults.BeyondBoundsPageCount,
        verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
        pagerIndicator: @Composable ((currentPage: Int) -> Unit)? = null,
        pageContent: @Composable PagerScope.(page: Int) -> Unit = { page ->
            MegaText(
                text = page.toString(),
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center
            )
        },
    ) {
        setContent {
            InfiniteHorizontalPagerWithIndicator(
                pageCount = pageCount,
                isOverScrollModeEnable = isOverScrollModeEnable,
                beyondBoundsPageCount = beyondBoundsPageCount,
                verticalAlignment = verticalAlignment,
                pagerIndicator = pagerIndicator,
                pageContent = pageContent
            )
        }
    }
}
