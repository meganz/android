package mega.privacy.android.feature.myaccount.presentation.widget.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyAccountHorizontalProgressBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that progress bar is displayed with success level`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Success,
                progress = 50f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar is displayed with warning level`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Warning,
                progress = 85f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar is displayed with error level`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Error,
                progress = 95f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar handles progress above 100`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Error,
                progress = 150f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar handles negative progress`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Success,
                progress = -10f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar handles zero progress`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Success,
                progress = 0f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar handles full progress`() {
        composeTestRule.setContent {
            MyAccountHorizontalProgressBar(
                level = QuotaLevel.Error,
                progress = 100f
            )
        }

        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }
}
