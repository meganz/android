package mega.privacy.android.app.presentation.audiosection.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ToolbarClickHandler
import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AudioSectionTopBarTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    private val handler = mock<NodeActionHandler>()

    private val selectAllControl: ToolbarClickHandler = @Composable { _, _, _, _ -> { } }

    @After
    fun tearDown() {
        reset(handler)
    }

    private fun setComposeContent(
        title: String = "Audio",
        isActionMode: Boolean = false,
        selectedSize: Int = 0,
        onSearchClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        menuItems: List<ToolbarMenuItem> = emptyList(),
        clearSelection: () -> Unit = {},
        selectAllAction: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AudioSectionTopBar(
                title = title,
                isActionMode = isActionMode,
                selectedSize = selectedSize,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed,
                menuItems = menuItems,
                handler = handler,
                navHostController = navController,
                clearSelection = clearSelection,
                selectAllAction = selectAllAction,
            )
        }
    }

    @Test
    fun `test that search top bar is displayed when isActionMode is false`() {
        setComposeContent(isActionMode = false)

        composeTestRule.onNodeWithTag(AUDIO_SECTION_SEARCH_TOP_BAR_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            AUDIO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `test that selected mode top bar is displayed when isActionMode is true`() {
        setComposeContent(isActionMode = true, selectedSize = 3)

        composeTestRule.onNodeWithTag(
            AUDIO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AUDIO_SECTION_SEARCH_TOP_BAR_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that selected mode top bar shows selected size as title`() {
        setComposeContent(isActionMode = true, selectedSize = 5)

        composeTestRule.onNodeWithText("5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that onBackPressed is invoked when back button is clicked in search mode`() {
        val onBackPressed = mock<() -> Unit>()
        setComposeContent(isActionMode = false, onBackPressed = onBackPressed)

        composeTestRule.onNodeWithTag("search_toolbar:back_button", useUnmergedTree = true)
            .performClick()

        verify(onBackPressed).invoke()
    }

    @Test
    fun `test that onBackPressed is invoked when back button is clicked in action mode`() {
        val onBackPressed = mock<() -> Unit>()
        setComposeContent(isActionMode = true, onBackPressed = onBackPressed)

        composeTestRule.onNodeWithTag("appbar:button_back", useUnmergedTree = true)
            .performClick()

        verify(onBackPressed).invoke()
    }

    @Test
    fun `test that onSearchClicked is invoked when search button is clicked`() {
        val onSearchClicked = mock<() -> Unit>()
        setComposeContent(isActionMode = false, onSearchClicked = onSearchClicked)

        composeTestRule.onNodeWithTag("search_toolbar:search_button", useUnmergedTree = true)
            .performClick()

        verify(onSearchClicked).invoke()
    }

    @Test
    fun `test that selectAllAction is invoked when select all menu action is clicked`() {
        val selectAllAction = mock<() -> Unit>()
        val menuItems = listOf(
            ToolbarMenuItem(
                action = SelectAllMenuAction(),
                control = selectAllControl,
            )
        )
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            menuItems = menuItems,
            selectAllAction = selectAllAction,
        )

        composeTestRule.onNodeWithTag("menuActionsShowMore", useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithTag("menu_type:select_all", useUnmergedTree = true)
            .performClick()

        verify(selectAllAction).invoke()
    }

    @Test
    fun `test that search top bar displays title when isActionMode is false`() {
        val title = "Audio Section"
        setComposeContent(title = title, isActionMode = false)

        composeTestRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }
}
