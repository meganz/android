package mega.privacy.android.core.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MinimumTimeVisibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the content is not added if wait duration is not reached`() = runTest {
        setContent()
        advanceTimeBy((WAIT_MILLIS_TO_SHOW / 2))
        composeTestRule.onNodeWithTag(TAG, true).assertDoesNotExist()
    }

    @Test
    fun `test that the content is added when wait duration is reached`() = runTest {
        setContent()
        advanceTimeBy((WAIT_MILLIS_TO_SHOW + MINIMUM_SHOW_MILLIS / 2))
        composeTestRule.onNodeWithTag(TAG, true).assertExists()
    }

    @Test
    fun `test that the content is not hidden if minimum duration is not reached`() = runTest {
        setContent(WAIT_MILLIS_TO_SHOW + 1)
        advanceTimeBy(WAIT_MILLIS_TO_SHOW + MINIMUM_SHOW_MILLIS / 2)
        composeTestRule.onNodeWithTag(TAG, true).assertExists()
    }

    @Test
    fun `test that the content is hidden when minimum duration is reached`() = runTest {
        setContent(WAIT_MILLIS_TO_SHOW + 1)
        advanceTimeBy(WAIT_MILLIS_TO_SHOW + MINIMUM_SHOW_MILLIS)
        composeTestRule.onNodeWithTag(TAG, true).assertDoesNotExist()
    }

    @Test
    fun `test that the content is hidden immediately if minimum duration it's already reached`() =
        runTest {
            val completeCycle = WAIT_MILLIS_TO_SHOW + MINIMUM_SHOW_MILLIS + 1
            setContent(completeCycle)
            advanceTimeBy(completeCycle)
            composeTestRule.onNodeWithTag(TAG, true).assertDoesNotExist()
        }

    private fun setContent(visibleFor: Long? = null) {
        composeTestRule.setContent {
            var visible by remember { mutableStateOf(true) }
            MinimumTimeVisibility(
                visible = visible,
                waitDurationToShow = WAIT_MILLIS_TO_SHOW.toDuration(DurationUnit.MILLISECONDS),
                minimumShowedDuration = MINIMUM_SHOW_MILLIS.toDuration(DurationUnit.MILLISECONDS),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(TAG)
                )
            }
            visibleFor?.let {
                LaunchedEffect(Unit) {
                    delay(it)
                    visible = false
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = false
    }

    private fun advanceTimeBy(timeInMillis: Long) {
        composeTestRule.mainClock.advanceTimeByFrame() //to trigger recomposition if needed
        composeTestRule.mainClock.advanceTimeBy((timeInMillis))
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    companion object {
        private const val TAG = "contentTag"
        private const val WAIT_MILLIS_TO_SHOW = 500L
        private const val MINIMUM_SHOW_MILLIS = 800L
    }
}