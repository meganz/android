package mega.privacy.android.app.appstate.content.navigation.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.navigation.contract.state.LocalBottomNavigationVisible
import mega.privacy.android.navigation.contract.state.LocalNavigationRailVisible
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CustomiseNavigationTooltipTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val attachedCoordinates: LayoutCoordinates = mock {
        on { it.isAttached }.thenReturn(true)
    }

    private val detachedCoordinates: LayoutCoordinates = mock {
        on { it.isAttached }.thenReturn(false)
    }

    @Composable
    private fun TooltipUnderTest(
        showTooltip: Boolean,
        anchorCoordinates: LayoutCoordinates?,
        isBottomNavigationVisible: Boolean = true,
        isNavigationRailVisible: Boolean = false,
        onDisplayed: () -> Unit = {},
    ) {
        AndroidThemeForPreviews {
            CompositionLocalProvider(
                LocalBottomNavigationVisible provides isBottomNavigationVisible,
                LocalNavigationRailVisible provides isNavigationRailVisible,
            ) {
                CustomiseNavigationTooltip(
                    showTooltip = showTooltip,
                    anchorCoordinates = anchorCoordinates,
                    onDisplayed = onDisplayed,
                    onDismiss = {},
                    onExplore = {},
                )
            }
        }
    }

    @Test
    fun `test that tooltip is displayed when visible and anchor coordinates are attached`() {
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = true,
                anchorCoordinates = attachedCoordinates,
            )
        }

        composeRule.onNodeWithTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that tooltip is not displayed when showTooltip is false`() {
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = false,
                anchorCoordinates = attachedCoordinates,
            )
        }

        composeRule.onNodeWithTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when anchor coordinates are null`() {
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = true,
                anchorCoordinates = null,
            )
        }

        composeRule.onNodeWithTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when anchor coordinates are detached`() {
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = true,
                anchorCoordinates = detachedCoordinates,
            )
        }

        composeRule.onNodeWithTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when bottom navigation is not visible`() {
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = true,
                anchorCoordinates = attachedCoordinates,
                isBottomNavigationVisible = false,
            )
        }

        composeRule.onNodeWithTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when navigation rail is visible`() {
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = true,
                anchorCoordinates = attachedCoordinates,
                isBottomNavigationVisible = true,
                isNavigationRailVisible = true,
            )
        }

        composeRule.onNodeWithTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that onDisplayed is invoked when the tooltip is displayed`() {
        var displayed = false
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = true,
                anchorCoordinates = attachedCoordinates,
                onDisplayed = { displayed = true },
            )
        }

        composeRule.waitForIdle()
        assertThat(displayed).isTrue()
    }

    @Test
    fun `test that onDisplayed is not invoked when the tooltip is hidden`() {
        var displayed = false
        composeRule.setContent {
            TooltipUnderTest(
                showTooltip = false,
                anchorCoordinates = attachedCoordinates,
                onDisplayed = { displayed = true },
            )
        }

        composeRule.waitForIdle()
        assertThat(displayed).isFalse()
    }
}
