package mega.privacy.mobile.home.presentation.configuration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import mega.privacy.mobile.home.presentation.configuration.model.WidgetConfigurationItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class HomeConfigurationScreenTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val moreActionTestTag = "node_selection_action:more"
    private val navigationIconContentDescription = "Navigation Icon"

    private val widgetA = WidgetConfigurationItem(
        identifier = "widget_a",
        index = 0,
        name = LocalizedText.Literal("Widget A"),
        enabled = true,
        canDelete = true,
    )

    private val widgetB = WidgetConfigurationItem(
        identifier = "widget_b",
        index = 1,
        name = LocalizedText.Literal("Widget B"),
        enabled = false,
        canDelete = true,
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
                widgets = listOf(widgetA, widgetB),
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
                widgets = listOf(widgetA, widgetB),
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
                widgets = listOf(widgetA, widgetB),
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
                widgets = listOf(widgetA),
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
                widgets = listOf(widgetA),
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
                widgets = listOf(widgetB),
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
                widgets = listOf(widgetA),
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
                widgets = listOf(widgetA),
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
                widgets = listOf(widgetA),
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
                widgets = listOf(widgetA),
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
}
