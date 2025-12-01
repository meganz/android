package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TimelineFilterViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the correct filters are applied`() {
        val currentFilter = TimelineFilterUiState(
            isRemembered = Random.nextBoolean(),
            mediaType = FilterMediaType.entries.random(),
            mediaSource = FilterMediaSource.entries.random()
        )
        composeRuleScope {
            val onApplyFilterClick = mock<(request: TimelineFilterRequest) -> Unit>()
            setView(
                currentFilter = currentFilter,
                onApplyFilterClick = onApplyFilterClick
            )

            onNodeWithTag(TIMELINE_FILTER_VIEW_APPLY_FILTER_BUTTON_TAG).performClick()

            verify(onApplyFilterClick).invoke(
                TimelineFilterRequest(
                    isRemembered = currentFilter.isRemembered,
                    mediaType = currentFilter.mediaType,
                    mediaSource = currentFilter.mediaSource
                )
            )
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setView(
        currentFilter: TimelineFilterUiState = TimelineFilterUiState(),
        onApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
        onClose: () -> Unit = {},
    ) {
        setContent {
            TimelineFilterView(
                currentFilter = currentFilter,
                onApplyFilterClick = onApplyFilterClick,
                onClose = onClose,
            )
        }
    }
}
