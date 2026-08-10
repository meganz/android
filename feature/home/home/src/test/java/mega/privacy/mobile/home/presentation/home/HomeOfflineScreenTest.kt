package mega.privacy.mobile.home.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeOfflineScreenTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that icon is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeOfflineScreen(
                    hasOfflineFiles = false,
                    onViewOfflineFilesClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_OFFLINE_ICON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that correct description is displayed when hasOfflineFiles is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeOfflineScreen(
                    hasOfflineFiles = false,
                    onViewOfflineFilesClick = {},
                )
            }
        }

        val expectedText = context.getString(sharedR.string.home_screen_no_network_desc)
        composeRule.onNodeWithText(expectedText, useUnmergedTree = true)
            .assertIsDisplayed()

        val textWithFiles = context.getString(sharedR.string.home_screen_no_network_desc_with_offline_files)
        composeRule.onNodeWithText(textWithFiles, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that correct description is displayed when hasOfflineFiles is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeOfflineScreen(
                    hasOfflineFiles = true,
                    onViewOfflineFilesClick = {},
                )
            }
        }

        val expectedText = context.getString(sharedR.string.home_screen_no_network_desc_with_offline_files)
        composeRule.onNodeWithText(expectedText, useUnmergedTree = true)
            .assertIsDisplayed()

        val textWithoutFiles = context.getString(sharedR.string.home_screen_no_network_desc)
        composeRule.onNodeWithText(textWithoutFiles, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that view offline files button is displayed when hasOfflineFiles is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeOfflineScreen(
                    hasOfflineFiles = true,
                    onViewOfflineFilesClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_OFFLINE_VIEW_FILES_BUTTON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that view offline files button is not displayed when hasOfflineFiles is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeOfflineScreen(
                    hasOfflineFiles = false,
                    onViewOfflineFilesClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_OFFLINE_VIEW_FILES_BUTTON_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that onViewOfflineFilesClick is called when button is clicked`() {
        var clicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeOfflineScreen(
                    hasOfflineFiles = true,
                    onViewOfflineFilesClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithTag(HOME_OFFLINE_VIEW_FILES_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(clicked).isTrue()
    }
}

