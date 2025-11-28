package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.home.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentDateHeaderTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that date header is displayed`() {
        val timestamp = System.currentTimeMillis() / 1000

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentDateHeader(timestamp = timestamp)
            }
        }

        composeRule.onNodeWithTag(DATE_HEADER_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that date header displays today for current timestamp`() {
        val timestamp = System.currentTimeMillis() / 1000

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentDateHeader(timestamp = timestamp)
            }
        }

        val expectedText = context.getString(R.string.label_today)
        composeRule.onNodeWithTag(DATE_HEADER_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals(expectedText)
    }

    @Test
    fun `test that date header displays yesterday for yesterday timestamp`() {
        val yesterdayTimestamp = System.currentTimeMillis() / 1000 - 86400 // 24 hours ago

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentDateHeader(timestamp = yesterdayTimestamp)
            }
        }

        val expectedText = context.getString(R.string.label_yesterday)
        composeRule.onNodeWithTag(DATE_HEADER_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals(expectedText)
    }

    @Test
    fun `test that date header displays formatted date for older timestamp`() {
        val fixedTimestamp = 1764141937L // Wed, 26 Nov 2025 07:25:37 GMT

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentDateHeader(timestamp = fixedTimestamp)
            }
        }

        composeRule.onNodeWithTag(DATE_HEADER_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Wednesday, Nov 26, 2025")
    }
}

