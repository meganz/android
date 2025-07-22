package mega.privacy.android.core.nodecomponents.list.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class NodeHeaderItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that sort order section is displayed when showSortOrder is true`() {
        val sortOrder = "Name"
        val onSortOrderClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = true,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithText(sortOrder).assertIsDisplayed()
    }

    @Test
    fun `test that sort order section is not displayed when showSortOrder is false`() {
        val sortOrder = "Name"

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithText(sortOrder).assertIsNotDisplayed()
    }

    @Test
    fun `test that sort order click invokes callback`() {
        val onSortOrderClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = true,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithText("Name").performClick()
        verify(onSortOrderClick).invoke()
    }

    @Test
    fun `test that grid view toggle is displayed when showChangeViewType is true and isListView is true`() {
        val onChangeViewTypeClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LIST_VIEW_TOGGLE_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that view type toggles are not displayed when showChangeViewType is false`() {
        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(LIST_VIEW_TOGGLE_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that grid view toggle click invokes callback`() {
        val onChangeViewTypeClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG).performClick()
        verify(onChangeViewTypeClick).invoke()
    }

    @Test
    fun `test that media discovery button is displayed when showMediaDiscoveryButton is true`() {
        val onEnterMediaDiscoveryClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = true
            )
        }

        composeTestRule.onNodeWithTag(MEDIA_DISCOVERY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that media discovery button is not displayed when showMediaDiscoveryButton is false`() {
        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(MEDIA_DISCOVERY_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that media discovery button click invokes callback`() {
        val onEnterMediaDiscoveryClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = true
            )
        }

        composeTestRule.onNodeWithTag(MEDIA_DISCOVERY_TAG).performClick()
        verify(onEnterMediaDiscoveryClick).invoke()
    }

    @Test
    fun `test that all elements are displayed when all flags are true`() {
        val onSortOrderClick = mock<() -> Unit>()
        val onChangeViewTypeClick = mock<() -> Unit>()
        val onEnterMediaDiscoveryClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                sortOrder = "Name",
                isListView = true,
                showSortOrder = true,
                showChangeViewType = true,
                showMediaDiscoveryButton = true
            )
        }

        composeTestRule.onNodeWithText("Name").assertIsDisplayed()
        composeTestRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(MEDIA_DISCOVERY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that no elements are displayed when all flags are false`() {
        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithText("Name").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(LIST_VIEW_TOGGLE_TAG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(MEDIA_DISCOVERY_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that sort order dropdown arrow is displayed when showSortOrder is true`() {
        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = true,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(SORT_ORDER_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that sort order dropdown arrow is not displayed when showSortOrder is false`() {
        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithText("DropDown arrow").assertIsNotDisplayed()
    }

    @Test
    fun `test that callbacks are not invoked when elements are not displayed`() {
        val onSortOrderClick = mock<() -> Unit>()
        val onChangeViewTypeClick = mock<() -> Unit>()
        val onEnterMediaDiscoveryClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                sortOrder = "Name",
                isListView = true,
                showSortOrder = false,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        // Since elements are not displayed, callbacks should not be invoked
        verifyNoInteractions(onSortOrderClick)
        verifyNoInteractions(onChangeViewTypeClick)
        verifyNoInteractions(onEnterMediaDiscoveryClick)
    }

    @Test
    fun `test that different sort order text is displayed correctly`() {
        val sortOrder = "Date Modified"

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = true,
                showChangeViewType = false,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithText(sortOrder).assertIsDisplayed()
    }

    @Test
    fun `test that list view toggle is displayed when showChangeViewType is true and isListView is false`() {
        val onChangeViewTypeClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = false,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(LIST_VIEW_TOGGLE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that list view toggle click invokes callback`() {
        val onChangeViewTypeClick = mock<() -> Unit>()

        composeTestRule.setContent {
            NodeHeaderItem(
                onSortOrderClick = {},
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = "Name",
                isListView = false,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false
            )
        }

        composeTestRule.onNodeWithTag(LIST_VIEW_TOGGLE_TAG).performClick()
        verify(onChangeViewTypeClick).invoke()
    }
}