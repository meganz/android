package mega.privacy.android.feature.chat.giphy.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.feature.chat.giphy.model.GiphyEmptyState
import mega.privacy.android.feature.chat.giphy.model.GiphyPickerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class GiphyPickerViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val gifs = List(3) {
        Giphy(
            mp4Src = null,
            webpSrc = null,
            title = "GIF $it",
            mp4Size = 0,
            webpSize = 0,
            width = 200,
            height = 100,
        )
    }

    private fun initComposeRuleContent(
        uiState: GiphyPickerUiState,
        onLoadMore: () -> Unit = {},
        onGifSelected: (Giphy) -> Unit = {},
    ) {
        composeRule.setContent {
            GiphyPickerView(
                uiState = uiState,
                onQueryChanged = {},
                onLoadMore = onLoadMore,
                onGifSelected = onGifSelected,
                onBack = {},
            )
        }
    }

    @Test
    fun `test that loading indicator is shown when there are no gifs yet`() {
        initComposeRuleContent(GiphyPickerUiState(isLoading = true))

        composeRule.onNodeWithTag(GIPHY_PICKER_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that gifs are shown when they are loaded`() {
        initComposeRuleContent(loadedState())

        composeRule.onNodeWithTag(GIPHY_PICKER_GRID_TAG).assertIsDisplayed()
        gifs.indices.forEach {
            composeRule.onNodeWithTag("$GIPHY_PICKER_ITEM_TAG$it").assertIsDisplayed()
        }
    }

    @Test
    fun `test that empty view is shown when there are no results`() {
        initComposeRuleContent(
            GiphyPickerUiState(isLoading = false, emptyState = GiphyEmptyState.NoResults)
        )

        composeRule.onNodeWithTag(GIPHY_PICKER_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is shown when the server is down`() {
        initComposeRuleContent(
            GiphyPickerUiState(isLoading = false, emptyState = GiphyEmptyState.ServerDown)
        )

        composeRule.onNodeWithTag(GIPHY_PICKER_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that end of results is shown when end of list is reached`() {
        initComposeRuleContent(loadedState(isEndOfList = true))

        composeRule.onNodeWithTag(GIPHY_PICKER_END_OF_RESULTS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onGifSelected is invoked when a gif is clicked`() {
        val onGifSelected = mock<(Giphy) -> Unit>()
        initComposeRuleContent(loadedState(), onGifSelected = onGifSelected)

        composeRule.onNodeWithTag("${GIPHY_PICKER_ITEM_TAG}1").performClick()

        verify(onGifSelected).invoke(gifs[1])
    }

    @Test
    fun `test that onLoadMore is invoked when the last gifs are visible`() {
        val onLoadMore = mock<() -> Unit>()
        initComposeRuleContent(loadedState(), onLoadMore = onLoadMore)

        composeRule.waitForIdle()

        verify(onLoadMore).invoke()
    }

    @Test
    fun `test that onLoadMore is not invoked when end of list is reached`() {
        val onLoadMore = mock<() -> Unit>()
        initComposeRuleContent(loadedState(isEndOfList = true), onLoadMore = onLoadMore)

        composeRule.waitForIdle()

        verify(onLoadMore, never()).invoke()
    }

    private fun loadedState(isEndOfList: Boolean = false) = GiphyPickerUiState(
        gifs = gifs.toImmutableList(),
        isLoading = false,
        isEndOfList = isEndOfList,
    )
}
