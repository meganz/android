package mega.privacy.android.feature.clouddrive.presentation.search.view

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SearchFilterChipsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsTracker: AnalyticsTracker = mock()

    @Before
    fun setup() {
        Analytics.initialise(analyticsTracker)
    }

    @Test
    fun `test that all filter chips are displayed`() {
        setupComposeContent()

        composeRule.onNodeWithTag(TYPE_FILTER_CHIP_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(DATE_MODIFIED_FILTER_CHIP_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(DATE_ADDED_FILTER_CHIP_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onFilterClicked is called with TYPE when type chip is clicked`() {
        val onFilterClicked = mock<(FilterType) -> Unit>()
        setupComposeContent(onFilterClicked = onFilterClicked)

        composeRule.onNodeWithTag(TYPE_FILTER_CHIP_TAG).performClick()

        verify(onFilterClicked).invoke(FilterType.TYPE)
    }

    @Test
    fun `test that onFilterClicked is called with LAST_MODIFIED when date modified chip is clicked`() {
        val onFilterClicked = mock<(FilterType) -> Unit>()
        setupComposeContent(onFilterClicked = onFilterClicked)

        composeRule.onNodeWithTag(DATE_MODIFIED_FILTER_CHIP_TAG).performClick()

        verify(onFilterClicked).invoke(FilterType.LAST_MODIFIED)
    }

    @Test
    fun `test that onFilterClicked is called with DATE_ADDED when date added chip is clicked`() {
        val onFilterClicked = mock<(FilterType) -> Unit>()
        setupComposeContent(onFilterClicked = onFilterClicked)

        composeRule.onNodeWithTag(DATE_ADDED_FILTER_CHIP_TAG).performClick()

        verify(onFilterClicked).invoke(FilterType.DATE_ADDED)
    }


    private fun setupComposeContent(
        typeFilterOption: TypeFilterOption? = null,
        dateModifiedFilterOption: DateFilterOption? = null,
        dateAddedFilterOption: DateFilterOption? = null,
        enabled: Boolean = true,
        onFilterClicked: (FilterType) -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                SearchFilterChips(
                    typeFilterOption = typeFilterOption,
                    dateModifiedFilterOption = dateModifiedFilterOption,
                    dateAddedFilterOption = dateAddedFilterOption,
                    onFilterClicked = onFilterClicked,
                    enabled = enabled,
                )
            }
        }
    }
}

