package mega.privacy.android.feature.clouddrive.presentation.search

import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterType
import mega.privacy.mobile.analytics.event.SearchDateAddedDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedDropdownChipPressedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SearchFilterAnalyticsTest {

    private val analyticsTracker: AnalyticsTracker = mock()

    @BeforeAll
    fun setUp() {
        Analytics.initialise(analyticsTracker)
    }

    @AfterEach
    fun tearDown() {
        reset(analyticsTracker)
    }

    @Test
    fun `test that pressing the type chip tracks the file type event`() {
        trackFilterChipPressed(SearchFilterType.TYPE.name)

        verify(analyticsTracker).trackEvent(SearchFileTypeDropdownChipPressedEvent)
    }

    @Test
    fun `test that pressing the last modified chip tracks the last modified event`() {
        trackFilterChipPressed(SearchFilterType.LAST_MODIFIED.name)

        verify(analyticsTracker).trackEvent(SearchLastModifiedDropdownChipPressedEvent)
    }

    @Test
    fun `test that pressing the date added chip tracks the date added event`() {
        trackFilterChipPressed(SearchFilterType.DATE_ADDED.name)

        verify(analyticsTracker).trackEvent(SearchDateAddedDropdownChipPressedEvent)
    }

    @Test
    fun `test that an unknown filter id tracks no event`() {
        trackFilterChipPressed(UNKNOWN_FILTER_ID)

        verifyNoInteractions(analyticsTracker)
    }

    private companion object {
        const val UNKNOWN_FILTER_ID = "unknown"
    }
}
