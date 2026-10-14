package mega.privacy.mobile.home.presentation.home

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.WidgetCustomisationTooltipDismissButtonPressedEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationTooltipDisplayedEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationTooltipExploreButtonPressedEvent
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class HomeConfigurationTooltipTest {

    private val composeRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val attachedCoordinates: LayoutCoordinates = mock {
        on { it.isAttached }.thenReturn(true)
    }

    @Test
    fun `test that tooltip is not displayed when state is Loading`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Loading,
                    iconCoordinates = attachedCoordinates,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when state is Offline`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Offline(hasOfflineFiles = false),
                    iconCoordinates = attachedCoordinates,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when showHomeConfigurationTooltip is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        showHomeConfigurationTooltip = false,
                    ),
                    iconCoordinates = attachedCoordinates,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when iconCoordinates is null`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        showHomeConfigurationTooltip = true,
                    ),
                    iconCoordinates = null,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is not displayed when iconCoordinates is detached`() {
        val detachedCoordinates: LayoutCoordinates = mock {
            on { it.isAttached }.thenReturn(false)
        }

        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        showHomeConfigurationTooltip = true,
                    ),
                    iconCoordinates = detachedCoordinates,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tooltip is displayed when all conditions are met`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        showHomeConfigurationTooltip = true,
                    ),
                    iconCoordinates = attachedCoordinates,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that tooltip displayed event is tracked when the tooltip is displayed`() {
        setVisibleTooltip()

        composeRule.onNodeWithTag(HOME_CONFIGURATION_TOOLTIP_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        assertThat(analyticsRule.events).containsExactly(WidgetCustomisationTooltipDisplayedEvent)
    }

    @Test
    fun `test that tooltip displayed event is not tracked when the tooltip is not displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        showHomeConfigurationTooltip = false,
                    ),
                    iconCoordinates = attachedCoordinates,
                    onDismiss = {},
                    onNavigateToConfiguration = {},
                )
            }
        }
        composeRule.waitForIdle()

        assertThat(analyticsRule.events).isEmpty()
    }

    @Test
    fun `test that tooltip explore event is tracked when the action button is clicked`() {
        var navigated = false
        setVisibleTooltip(onNavigateToConfiguration = { navigated = true })
        composeRule.waitForIdle()
        analyticsRule.events.clear()

        composeRule.onNodeWithText(
            context.getString(sharedR.string.home_configuration_tooltip_action),
            useUnmergedTree = true,
        ).performClick()

        assertThat(navigated).isTrue()
        assertThat(analyticsRule.events)
            .containsExactly(WidgetCustomisationTooltipExploreButtonPressedEvent)
    }

    @Test
    fun `test that tooltip dismiss event is tracked when the close icon is clicked`() {
        var dismissed = false
        setVisibleTooltip(onDismiss = { dismissed = true })
        composeRule.waitForIdle()
        analyticsRule.events.clear()

        composeRule.onNodeWithContentDescription("Close", useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()

        assertThat(dismissed).isTrue()
        assertThat(analyticsRule.events)
            .containsExactly(WidgetCustomisationTooltipDismissButtonPressedEvent)
    }

    private fun setVisibleTooltip(
        onDismiss: () -> Unit = {},
        onNavigateToConfiguration: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        showHomeConfigurationTooltip = true,
                    ),
                    iconCoordinates = attachedCoordinates,
                    onDismiss = onDismiss,
                    onNavigateToConfiguration = onNavigateToConfiguration,
                )
            }
        }
    }
}
