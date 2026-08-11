package mega.privacy.android.app.presentation.settings.customisenavigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.app.presentation.settings.customisenavigation.model.CustomiseNavigationUiState
import mega.privacy.android.app.presentation.settings.customisenavigation.model.NavigationItemUiModel
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w480dp-h1200dp")
class CustomiseNavigationScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val homeItem = NavigationItemUiModel(
        id = "home",
        label = sharedR.string.general_section_home,
        icon = IconPack.Medium.Thin.Outline.Home,
    )
    private val driveItem = NavigationItemUiModel(
        id = "drive",
        label = sharedR.string.general_drive,
        icon = IconPack.Medium.Thin.Outline.Folder,
    )
    private val mediaItem = NavigationItemUiModel(
        id = "media",
        label = sharedR.string.media_feature_title,
        icon = IconPack.Medium.Thin.Outline.Image01,
    )
    private val chatItem = NavigationItemUiModel(
        id = "chat",
        label = sharedR.string.general_chat,
        icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
    )
    private val sharesItem = NavigationItemUiModel(
        id = "shares",
        label = sharedR.string.video_section_videos_location_option_shared_items,
        icon = IconPack.Medium.Thin.Outline.FolderUsers,
    )
    private val menuItem = NavigationItemUiModel(
        id = "menu",
        label = sharedR.string.general_menu,
        icon = IconPack.Medium.Thin.Outline.Menu01,
    )

    @Before
    fun setUp() {
        Analytics.initialise(mock<AnalyticsTracker>())
    }

    @Test
    fun `test that available item moves into your navigation before the menu row when its toggle is switched on`() {
        setScreenContent()

        toggleFor(chatItem.id).performScrollTo().performClick()

        rowIn(CustomiseNavigationScreenTestTags.YOUR_NAVIGATION_CARD, chatItem.id).assertExists()
        rowIn(CustomiseNavigationScreenTestTags.AVAILABLE_TO_ADD_CARD, chatItem.id)
            .assertDoesNotExist()
        assertYourNavigationOrder(
            homeItem.id,
            driveItem.id,
            mediaItem.id,
            chatItem.id,
            menuItem.id,
        )
    }

    @Test
    fun `test that item moves back to available to add when its toggle is switched off`() {
        setScreenContent()

        toggleFor(chatItem.id).performScrollTo().performClick()
        toggleFor(chatItem.id).performScrollTo().performClick()

        rowIn(CustomiseNavigationScreenTestTags.AVAILABLE_TO_ADD_CARD, chatItem.id).assertExists()
        rowIn(CustomiseNavigationScreenTestTags.YOUR_NAVIGATION_CARD, chatItem.id)
            .assertDoesNotExist()
        assertYourNavigationOrder(homeItem.id, driveItem.id, mediaItem.id, menuItem.id)
    }

    @Test
    fun `test that items counter shows the selected count including menu without error when within limits`() {
        setScreenContent()

        composeTestRule.onNodeWithTag(
            CustomiseNavigationScreenTestTags.ITEMS_COUNTER,
            useUnmergedTree = true,
        ).assertTextEquals("4 out of 5")
    }

    @Test
    fun `test that items counter shows 5 out of 5 when four items and the menu are selected`() {
        setScreenContent(
            selected = listOf(homeItem, driveItem, mediaItem, chatItem),
            available = listOf(sharesItem),
        )

        composeTestRule.onNodeWithTag(
            CustomiseNavigationScreenTestTags.ITEMS_COUNTER,
            useUnmergedTree = true,
        ).assertTextEquals("5 out of 5")
    }

    @Test
    fun `test that items counter exceeds the maximum when a fifth item is added`() {
        setScreenContent(
            selected = listOf(homeItem, driveItem, mediaItem, chatItem),
            available = listOf(sharesItem),
        )

        toggleFor(sharesItem.id).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(
            CustomiseNavigationScreenTestTags.ITEMS_COUNTER,
            useUnmergedTree = true,
        ).assertTextEquals("6 out of 5")
    }

    @Test
    fun `test that max items snackbar is shown and onSave is not invoked when saving with five items selected`() {
        val savedIds = mutableListOf<List<String>>()
        setScreenContent(
            selected = listOf(homeItem, driveItem, mediaItem, chatItem),
            available = listOf(sharesItem),
            onSave = { savedIds += it },
        )

        toggleFor(sharesItem.id).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .performClick()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                sharedR.string.settings_customise_navigation_max_items_snackbar,
                4,
            )
        ).assertIsDisplayed()
        assertThat(savedIds).isEmpty()
    }

    @Test
    fun `test that min items snackbar is shown and onSave is not invoked when saving with fewer than three items`() {
        val savedIds = mutableListOf<List<String>>()
        setScreenContent(onSave = { savedIds += it })

        toggleFor(mediaItem.id).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .performClick()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                sharedR.string.settings_customise_navigation_min_items_snackbar,
                3,
            )
        ).assertIsDisplayed()
        assertThat(savedIds).isEmpty()
    }

    @Test
    fun `test that dragging a row down one position reorders the pending arrangement`() {
        setScreenContent()

        rowFor(homeItem.id).performScrollTo().performDragDownByRows(1f)

        assertYourNavigationOrder(driveItem.id, homeItem.id, mediaItem.id, menuItem.id)
    }

    @Test
    fun `test that menu row is not reorderable and its toggle is disabled`() {
        setScreenContent()

        composeTestRule.onNode(
            isToggleable() and hasAnyAncestor(
                hasTestTag(CustomiseNavigationScreenTestTags.navigationItemRow(menuItem.id))
            ),
            useUnmergedTree = true,
        ).assertDoesNotExist()

        rowFor(menuItem.id).performScrollTo().performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, -height * 2f))
            up()
        }

        assertYourNavigationOrder(homeItem.id, driveItem.id, mediaItem.id, menuItem.id)
    }

    @Test
    fun `test that start screen subtitle is shown on the first row only`() {
        setScreenContent()

        val startScreen =
            composeTestRule.activity.getString(sharedR.string.settings_customise_navigation_start_screen)
        rowFor(homeItem.id).assert(hasAnyDescendant(hasText(startScreen)))
        composeTestRule.onNode(
            hasText(startScreen) and hasAnyAncestor(
                hasTestTag(CustomiseNavigationScreenTestTags.navigationItemRow(driveItem.id))
            ),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun `test that start screen subtitle moves to the new first row when reordered`() {
        setScreenContent()

        rowFor(homeItem.id).performScrollTo().performDragDownByRows(1f)

        val startScreen =
            composeTestRule.activity.getString(sharedR.string.settings_customise_navigation_start_screen)
        rowFor(driveItem.id).assert(hasAnyDescendant(hasText(startScreen)))
        composeTestRule.onNode(
            hasText(startScreen) and hasAnyAncestor(
                hasTestTag(CustomiseNavigationScreenTestTags.navigationItemRow(homeItem.id))
            ),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun `test that preview bar shows the pending items in order with the menu item last`() {
        setScreenContent()

        assertPreviewBarOrder(homeItem.id, driveItem.id, mediaItem.id, menuItem.id)
        previewBarItemFor(chatItem.id).assertDoesNotExist()
    }

    @Test
    fun `test that preview bar reflects the pending selection when an item is added`() {
        setScreenContent()

        toggleFor(chatItem.id).performScrollTo().performClick()

        assertPreviewBarOrder(
            homeItem.id,
            driveItem.id,
            mediaItem.id,
            chatItem.id,
            menuItem.id,
        )
    }

    @Test
    fun `test that preview bar reflects the pending order when a row is reordered`() {
        setScreenContent()

        rowFor(homeItem.id).performScrollTo().performDragDownByRows(1f)

        assertPreviewBarOrder(driveItem.id, homeItem.id, mediaItem.id, menuItem.id)
    }

    @Test
    fun `test that save button is disabled when there are no pending changes`() {
        setScreenContent()

        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun `test that save button is enabled when the pending arrangement changes`() {
        setScreenContent()

        toggleFor(chatItem.id).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .assertIsEnabled()
    }

    @Test
    fun `test that onSave is invoked with the pending ordered ids excluding menu when save is clicked`() {
        val savedIds = mutableListOf<List<String>>()
        setScreenContent(onSave = { savedIds += it })

        toggleFor(chatItem.id).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .performClick()

        assertThat(savedIds).containsExactly(
            listOf(homeItem.id, driveItem.id, mediaItem.id, chatItem.id)
        )
    }

    @Test
    fun `test that reset restores the default arrangement without invoking onSave`() {
        val savedIds = mutableListOf<List<String>>()
        setScreenContent(onSave = { savedIds += it })

        toggleFor(chatItem.id).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.RESET_BUTTON)
            .performClick()

        rowIn(CustomiseNavigationScreenTestTags.AVAILABLE_TO_ADD_CARD, chatItem.id).assertExists()
        assertYourNavigationOrder(homeItem.id, driveItem.id, mediaItem.id, menuItem.id)
        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .assertIsNotEnabled()
        assertThat(savedIds).isEmpty()
    }

    @Test
    fun `test that pending arrangement survives state restoration`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            AndroidThemeForPreviews {
                CustomiseNavigationScreen(
                    state = dataState(),
                    onBackPressed = {},
                    onSave = {},
                )
            }
        }
        toggleFor(chatItem.id).performScrollTo().performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        rowIn(CustomiseNavigationScreenTestTags.YOUR_NAVIGATION_CARD, chatItem.id).assertExists()
        assertYourNavigationOrder(
            homeItem.id,
            driveItem.id,
            mediaItem.id,
            chatItem.id,
            menuItem.id,
        )
        composeTestRule.onNodeWithTag(CustomiseNavigationScreenTestTags.SAVE_BUTTON)
            .assertIsEnabled()
    }

    private fun dataState(
        selected: List<NavigationItemUiModel> = listOf(homeItem, driveItem, mediaItem),
        available: List<NavigationItemUiModel> = listOf(chatItem, sharesItem),
    ) = CustomiseNavigationUiState.Data(
        baseArrangement = selected,
        availableItems = available,
        menuItem = menuItem,
        defaultArrangementIds = selected.map { it.id },
        savedEvent = consumed,
    )

    private fun setScreenContent(
        selected: List<NavigationItemUiModel> = listOf(homeItem, driveItem, mediaItem),
        available: List<NavigationItemUiModel> = listOf(chatItem, sharesItem),
        onSave: (List<String>) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CustomiseNavigationScreen(
                    state = dataState(selected = selected, available = available),
                    onBackPressed = {},
                    onSave = onSave,
                )
            }
        }
    }

    private fun rowFor(id: String) = composeTestRule.onNodeWithTag(
        CustomiseNavigationScreenTestTags.navigationItemRow(id),
        useUnmergedTree = true,
    )

    private fun rowIn(cardTag: String, id: String) = composeTestRule.onNode(
        hasTestTag(CustomiseNavigationScreenTestTags.navigationItemRow(id)) and
                hasAnyAncestor(hasTestTag(cardTag)),
        useUnmergedTree = true,
    )

    private fun toggleFor(id: String) = composeTestRule.onNode(
        isToggleable() and hasAnyAncestor(
            hasTestTag(CustomiseNavigationScreenTestTags.navigationItemRow(id))
        ),
        useUnmergedTree = true,
    )

    private fun previewBarItemFor(id: String) = composeTestRule.onNodeWithTag(
        CustomiseNavigationScreenTestTags.previewBarItem(id),
        useUnmergedTree = true,
    )

    private fun SemanticsNodeInteraction.performDragDownByRows(rows: Float) =
        performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, height * rows))
            up()
        }

    private fun assertYourNavigationOrder(vararg ids: String) {
        val rowTops = ids.map { id ->
            rowIn(CustomiseNavigationScreenTestTags.YOUR_NAVIGATION_CARD, id)
                .getUnclippedBoundsInRoot().top
        }
        assertThat(rowTops).isInOrder()
    }

    private fun assertPreviewBarOrder(vararg ids: String) {
        val itemLefts = ids.map { id -> previewBarItemFor(id).getUnclippedBoundsInRoot().left }
        assertThat(itemLefts).isInOrder()
    }
}
