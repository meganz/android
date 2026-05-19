package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinueWhereLeftOffCarouselTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val pdfItem = ContinueWhereLeftOffItem(
        nodeHandle = 1L,
        title = "Document.pdf",
        type = RecentlyUsedType.PDF,
        lastAccessedTimestamp = 1712880000L,
    )

    @Test
    fun `test that title is displayed when items is empty`() {
        setContent(items = emptyList())

        val title = context.getString(sharedR.string.home_widget_continue_where_left_off)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed when items is not empty`() {
        setContent(items = listOf(pdfItem))

        val title = context.getString(sharedR.string.home_widget_continue_where_left_off)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed when items is empty`() {
        setContent(items = emptyList())

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that empty view is not displayed when items is not empty`() {
        setContent(items = listOf(pdfItem))

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that chevron is hidden when items is empty`() {
        setContent(items = emptyList())

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that chevron is displayed when items is not empty`() {
        setContent(items = listOf(pdfItem))

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onViewAllClick is invoked when chevron is clicked`() {
        var viewAllClicked = false
        setContent(
            items = listOf(pdfItem),
            onViewAllClick = { viewAllClicked = true },
        )

        composeRule.onNodeWithTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG).performClick()

        assertThat(viewAllClicked).isTrue()
    }

    private fun setContent(
        items: List<ContinueWhereLeftOffItem>,
        onItemClick: (ContinueWhereLeftOffItem) -> Unit = {},
        onViewAllClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ContinueWhereLeftOffCarousel(
                    items = items,
                    onItemClick = onItemClick,
                    onViewAllClick = onViewAllClick,
                )
            }
        }
    }
}
