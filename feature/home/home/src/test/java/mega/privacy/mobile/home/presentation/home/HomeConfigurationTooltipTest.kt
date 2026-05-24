package mega.privacy.mobile.home.presentation.home

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class HomeConfigurationTooltipTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun `test that tooltip is not displayed when isHomeCustomizationEnabled is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        isHomeCustomizationEnabled = false,
                        showHomeConfigurationTooltip = true,
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
    fun `test that tooltip is not displayed when showHomeConfigurationTooltip is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationTooltip(
                    state = HomeUiState.Data(
                        widgets = emptyList(),
                        isHomeCustomizationEnabled = true,
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
                        isHomeCustomizationEnabled = true,
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
                        isHomeCustomizationEnabled = true,
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
                        isHomeCustomizationEnabled = true,
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
}
