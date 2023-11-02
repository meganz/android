package mega.privacy.android.core.ui.controls.layouts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class ScaffoldWithCollapsibleHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        hasHeaderBelowAppbar: Boolean = true,
    ) {
        composeTestRule.setContent {
            ScaffoldWithCollapsibleHeader(
                headerIncludingSystemBar = if (!hasHeaderBelowAppbar) null else {
                    @Composable {
                        Text(
                            "headerBelowAppBar",
                            modifier = Modifier
                                .testTag(HEADER_BELOW_APP_TAG)
                                .fillMaxSize()
                        )
                    }
                },
                topBar = {
                    AppBarForCollapsibleHeader(
                        appBarType = AppBarType.MENU,
                        title = "Title",
                        modifier = Modifier.testTag(TOOLBAR_APP_TAG),
                    )
                },
                header = @Composable {
                    Text(
                        "headerAboveAppBar",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(HEADER_ABOVE_APP_TAG)
                    )
                }
            ) {
                Text(
                    "content",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2000.dp)
                        .testTag(CONTENT_TAG)
                )
            }
        }
    }

    @Test
    fun `test that header below app bar is shown when scaffold is not scrolled`() {
        setContent()
        composeTestRule.onNodeWithTag(HEADER_BELOW_APP_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that header below app bar is hidden when scaffold is scrolled`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTENT_TAG).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(HEADER_BELOW_APP_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that header above app bar is shown when scaffold is not scrolled`() {
        setContent()
        composeTestRule.onNodeWithTag(HEADER_ABOVE_APP_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that header above app bar is hidden when scaffold is scrolled`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTENT_TAG).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(HEADER_ABOVE_APP_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that toolbar is shown when scaffold is scrolled`() {
        setContent()
        composeTestRule.onNodeWithTag(TOOLBAR_APP_TAG).assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(TOOLBAR_APP_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that content is shown when scaffold is scrolled`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
    }

    companion object {
        private const val HEADER_BELOW_APP_TAG = "scaffold:box_below"
        private const val HEADER_ABOVE_APP_TAG = "scaffold:box_above"
        private const val TOOLBAR_APP_TAG = "scaffold:toolbar"
        private const val CONTENT_TAG = "scaffold:content"
    }
}