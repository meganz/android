package mega.privacy.mobile.home.presentation.configuration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.WidgetCustomisationChooseDefaultStartScreenMenuItemEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationLastWidgetSnackbarDisplayedEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationMoreMenuToolbarEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationResetToDefaultMenuItemEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationScreenEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationWidgetDisabledEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationWidgetEnabledEvent
import mega.privacy.mobile.analytics.event.WidgetCustomisationWidgetsReorderedEvent
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import mega.privacy.mobile.home.presentation.configuration.model.WidgetConfigurationItem
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class HomeConfigurationScreenTest {

    private val composeRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val moreActionTestTag = "node_selection_action:more"
    private val navigationIconContentDescription = "Navigation Icon"

    private val widgetA = WidgetConfigurationItem(
        identifier = "widget_a",
        index = 0,
        name = LocalizedText.Literal("Widget A"),
        enabled = true,
        canDelete = true,
        isConfigurable = true,
        isDraggable = true,
    )

    private val widgetB = WidgetConfigurationItem(
        identifier = "widget_b",
        index = 1,
        name = LocalizedText.Literal("Widget B"),
        enabled = false,
        canDelete = true,
        isConfigurable = true,
        isDraggable = true,
    )

    private fun setContent(
        state: HomeConfigurationUiState,
        onWidgetEnabledChange: (WidgetConfigurationItem, Boolean) -> Unit = { _, _ -> },
        onWidgetOrderChange: (List<WidgetConfigurationItem>) -> Unit = {},
        showSnackbarMessage: (String) -> Unit = {},
        onBack: () -> Unit = {},
        onResetToDefault: () -> Unit = {},
        onChooseDefaultStartScreen: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                HomeConfigurationScreen(
                    state = state,
                    onWidgetEnabledChange = onWidgetEnabledChange,
                    onWidgetOrderChange = onWidgetOrderChange,
                    showSnackbarMessage = showSnackbarMessage,
                    onBack = onBack,
                    onResetToDefault = onResetToDefault,
                    onChooseDefaultStartScreen = onChooseDefaultStartScreen,
                )
            }
        }
    }

    @Test
    fun `test that title and subtitle are displayed`() {
        setContent(state = HomeConfigurationUiState.Loading)

        composeRule.onNodeWithText(
            context.getString(sharedR.string.home_configuration_screen_toolbar_title)
        ).assertIsDisplayed()

        composeRule.onNodeWithText(
            context.getString(sharedR.string.home_configuration_screen_toolbar_subtitle)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that widget configuration list is not displayed when state is Loading`() {
        setContent(state = HomeConfigurationUiState.Loading)

        composeRule.onNodeWithTag(TEST_TAG_WIDGET_CONFIGURATION_VIEW)
            .assertDoesNotExist()
    }

    @Test
    fun `test that more menu action is not displayed when state is Loading`() {
        setContent(state = HomeConfigurationUiState.Loading)

        composeRule.onNodeWithTag(moreActionTestTag)
            .assertDoesNotExist()
    }

    @Test
    fun `test that widget configuration list is displayed when state is Data`() {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA, widgetB),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithTag(TEST_TAG_WIDGET_CONFIGURATION_VIEW)
            .assertIsDisplayed()
    }

    @Test
    fun `test that widget items are displayed for each widget in Data state`() {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA, widgetB),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + widgetA.identifier,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + widgetB.identifier,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Widget A").assertIsDisplayed()
        composeRule.onNodeWithText("Widget B").assertIsDisplayed()
    }

    @Test
    fun `test that toggle reflects enabled state for each widget`() {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA, widgetB),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).assertIsOn()
        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetB.identifier,
            useUnmergedTree = true,
        ).assertIsOff()
    }

    @Test
    fun `test that more menu action is displayed when state is Data`() {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithTag(moreActionTestTag, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onBack is invoked when navigation icon is clicked`() {
        val onBack = mock<() -> Unit>()
        setContent(
            state = HomeConfigurationUiState.Loading,
            onBack = onBack,
        )

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .performClick()

        verify(onBack).invoke()
    }

    @Test
    fun `test that onWidgetEnabledChange is invoked with new value when toggle is clicked and allowRemoval is true`() {
        val onWidgetEnabledChange = mock<(WidgetConfigurationItem, Boolean) -> Unit>()
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            ),
            onWidgetEnabledChange = onWidgetEnabledChange,
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).performClick()

        verify(onWidgetEnabledChange).invoke(widgetA, false)
    }

    @Test
    fun `test that onWidgetEnabledChange is invoked when enabling a widget even if allowRemoval is false`() {
        val onWidgetEnabledChange = mock<(WidgetConfigurationItem, Boolean) -> Unit>()
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = false,
                draggableWidgets = listOf(widgetB),
                fixedWidgets = emptyList(),
            ),
            onWidgetEnabledChange = onWidgetEnabledChange,
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetB.identifier,
            useUnmergedTree = true,
        ).performClick()

        verify(onWidgetEnabledChange).invoke(widgetB, true)
    }

    @Test
    fun `test that onWidgetEnabledChange is not invoked when disabling and allowRemoval is false`() {
        val onWidgetEnabledChange = mock<(WidgetConfigurationItem, Boolean) -> Unit>()
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = false,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            ),
            onWidgetEnabledChange = onWidgetEnabledChange,
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).performClick()

        verifyNoInteractions(onWidgetEnabledChange)
    }

    @Test
    fun `test that showSnackbarMessage is invoked with removal not allowed message when disabling and allowRemoval is false`() {
        val showSnackbarMessage = mock<(String) -> Unit>()
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = false,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            ),
            showSnackbarMessage = showSnackbarMessage,
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).performClick()

        val expected =
            context.getString(sharedR.string.home_configuration_widget_removal_not_allowed_message)
        verify(showSnackbarMessage).invoke(expected)
    }

    @Test
    fun `test that showSnackbarMessage is not invoked when allowRemoval is true`() {
        val showSnackbarMessage = mock<(String) -> Unit>()
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            ),
            showSnackbarMessage = showSnackbarMessage,
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).performClick()

        verifyNoInteractions(showSnackbarMessage)
    }

    @Test
    fun `test that bottom sheet menu items are displayed when more action is clicked`() {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithTag(moreActionTestTag, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TEST_TAG_MENU_RESET_TO_DEFAULT, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_MENU_CHOOSE_DEFAULT_START_SCREEN, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that widget item is not displayed when isConfigurable is false`() {
        val nonConfigurableWidget = widgetA.copy(isConfigurable = false)
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(nonConfigurableWidget),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + nonConfigurableWidget.identifier,
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun `test that reorder icon is displayed when isDraggable is true`() {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = listOf(widgetA),
                fixedWidgets = emptyList(),
            )
        )

        composeRule.onNodeWithContentDescription("Reorder icon", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that reorder icon is not displayed for fixed widgets`() {
        val nonDraggableWidget = widgetA.copy(isDraggable = false)
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = emptyList(),
                fixedWidgets = listOf(nonDraggableWidget),
            )
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + nonDraggableWidget.identifier,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reorder icon", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that fixed widget items are displayed`() {
        val fixedWidget = widgetA.copy(identifier = "fixed_widget", isDraggable = false)
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = emptyList(),
                fixedWidgets = listOf(fixedWidget),
            )
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + fixedWidget.identifier,
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `test that fixed widget item is not displayed when isConfigurable is false`() {
        val nonConfigurableFixedWidget =
            widgetA.copy(isDraggable = false, isConfigurable = false)
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = true,
                draggableWidgets = emptyList(),
                fixedWidgets = listOf(nonConfigurableFixedWidget),
            )
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + nonConfigurableFixedWidget.identifier,
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun `test that screen event is tracked when the screen is displayed`() {
        setContent(state = HomeConfigurationUiState.Loading)
        composeRule.waitForIdle()

        assertThat(analyticsRule.events).containsExactly(WidgetCustomisationScreenEvent)
    }

    @Test
    fun `test that more menu toolbar event is tracked when more action is clicked`() {
        setDataContent(draggableWidgets = listOf(widgetA))

        composeRule.onNodeWithTag(moreActionTestTag, useUnmergedTree = true)
            .performClick()

        assertThat(analyticsRule.events).containsExactly(WidgetCustomisationMoreMenuToolbarEvent)
    }

    @Test
    fun `test that reset to default menu item event is tracked when reset to default is clicked`() {
        setDataContent(draggableWidgets = listOf(widgetA))
        openMoreMenu()

        composeRule.onNodeWithTag(TEST_TAG_MENU_RESET_TO_DEFAULT, useUnmergedTree = true)
            .performClick()

        assertThat(analyticsRule.events)
            .containsExactly(WidgetCustomisationResetToDefaultMenuItemEvent)
    }

    @Test
    fun `test that choose default start screen menu item event is tracked when choose default start screen is clicked`() {
        setDataContent(draggableWidgets = listOf(widgetA))
        openMoreMenu()

        composeRule.onNodeWithTag(TEST_TAG_MENU_CHOOSE_DEFAULT_START_SCREEN, useUnmergedTree = true)
            .performClick()

        assertThat(analyticsRule.events)
            .containsExactly(WidgetCustomisationChooseDefaultStartScreenMenuItemEvent)
    }

    @Test
    fun `test that widget disabled event is tracked with the widget identifier when a widget is turned off`() {
        setDataContent(draggableWidgets = listOf(widgetA, widgetB))

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).performClick()

        val event = analyticsRule.events
            .filterIsInstance<WidgetCustomisationWidgetDisabledEvent>()
            .single()
        assertThat(event.info["widget"]).isEqualTo(widgetA.identifier)
        assertThat(analyticsRule.events).hasSize(1)
    }

    @Test
    fun `test that widget enabled event is tracked with the widget identifier when a widget is turned on`() {
        setDataContent(draggableWidgets = listOf(widgetA, widgetB), allowRemoval = false)

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetB.identifier,
            useUnmergedTree = true,
        ).performClick()

        val event = analyticsRule.events
            .filterIsInstance<WidgetCustomisationWidgetEnabledEvent>()
            .single()
        assertThat(event.info["widget"]).isEqualTo(widgetB.identifier)
        assertThat(analyticsRule.events).hasSize(1)
    }

    @Test
    fun `test that last widget snackbar event is tracked when turning off the last enabled widget is blocked`() {
        setDataContent(draggableWidgets = listOf(widgetA), allowRemoval = false)

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM_TOGGLE + widgetA.identifier,
            useUnmergedTree = true,
        ).performClick()

        assertThat(analyticsRule.events)
            .containsExactly(WidgetCustomisationLastWidgetSnackbarDisplayedEvent)
    }

    @Test
    fun `test that widgets reordered event is tracked once when a drag changes the order`() {
        val onWidgetOrderChange = mock<(List<WidgetConfigurationItem>) -> Unit>()
        setDataContent(
            draggableWidgets = listOf(widgetA, widgetB),
            onWidgetOrderChange = onWidgetOrderChange,
        )

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + widgetA.identifier,
            useUnmergedTree = true,
        ).performTouchInput {
            down(center)
            advanceEventTime(1_000)
            repeat(10) { moveBy(Offset(0f, height / 4f)) }
            up()
        }
        composeRule.waitForIdle()

        verify(onWidgetOrderChange).invoke(listOf(widgetB, widgetA))
        assertThat(analyticsRule.events).containsExactly(WidgetCustomisationWidgetsReorderedEvent)
    }

    @Test
    fun `test that widgets reordered event is not tracked when a drag does not change the order`() {
        setDataContent(draggableWidgets = listOf(widgetA, widgetB))

        composeRule.onNodeWithTag(
            TEST_TAG_WIDGET_CONFIGURATION_ITEM + widgetA.identifier,
            useUnmergedTree = true,
        ).performTouchInput {
            down(center)
            advanceEventTime(1_000)
            up()
        }
        composeRule.waitForIdle()

        assertThat(analyticsRule.events).doesNotContain(WidgetCustomisationWidgetsReorderedEvent)
    }

    private fun setDataContent(
        draggableWidgets: List<WidgetConfigurationItem>,
        allowRemoval: Boolean = true,
        onWidgetOrderChange: (List<WidgetConfigurationItem>) -> Unit = {},
    ) {
        setContent(
            state = HomeConfigurationUiState.Data(
                allowRemoval = allowRemoval,
                draggableWidgets = draggableWidgets,
                fixedWidgets = emptyList(),
            ),
            onWidgetOrderChange = onWidgetOrderChange,
        )
        composeRule.waitForIdle()
        analyticsRule.events.clear()
    }

    private fun openMoreMenu() {
        composeRule.onNodeWithTag(moreActionTestTag, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        analyticsRule.events.clear()
    }
}
