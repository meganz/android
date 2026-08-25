package mega.privacy.android.feature.photos.presentation.timeline.revamp.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TIMELINE_SORT_OPTION_DATE_ADDED_TAG
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TIMELINE_SORT_OPTION_DATE_TAKEN_TAG
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampSortConfiguration
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampSortOption
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TimelineRevampSortBottomSheetTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Before
    fun setup() {
        Analytics.initialise(mock<AnalyticsTracker>())
    }

    private fun setContent(
        selected: TimelineRevampSortConfiguration = TimelineRevampSortConfiguration.Default,
        onSortChange: (TimelineRevampSortConfiguration) -> Unit = {},
        onDismissRequest: () -> Unit = {},
    ) {
        composeRule.setContent {
            TimelineRevampSortBottomSheet(
                selected = selected,
                onDismissRequest = onDismissRequest,
                onSortChange = onSortChange,
            )
        }
    }

    @Test
    fun `test that both sort options are displayed`() {
        setContent()

        composeRule.onNodeWithTag(TIMELINE_SORT_OPTION_DATE_TAKEN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_SORT_OPTION_DATE_ADDED_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that selecting Date taken emits it newest first`() {
        var result: TimelineRevampSortConfiguration? = null
        setContent(onSortChange = { result = it })

        composeRule.onNodeWithTag(TIMELINE_SORT_OPTION_DATE_TAKEN_TAG).performClick()

        assertThat(result).isEqualTo(
            TimelineRevampSortConfiguration(
                option = TimelineRevampSortOption.DateTaken,
                direction = SortDirection.Descending,
            )
        )
    }

    @Test
    fun `test that tapping the selected option toggles the direction and keeps the option`() {
        var result: TimelineRevampSortConfiguration? = null
        setContent(
            selected = TimelineRevampSortConfiguration(
                option = TimelineRevampSortOption.DateTaken,
                direction = SortDirection.Descending,
            ),
            onSortChange = { result = it },
        )

        composeRule.onNodeWithTag(TIMELINE_SORT_OPTION_DATE_TAKEN_TAG).performClick()

        assertThat(result).isEqualTo(
            TimelineRevampSortConfiguration(
                option = TimelineRevampSortOption.DateTaken,
                direction = SortDirection.Ascending,
            )
        )
    }
}
