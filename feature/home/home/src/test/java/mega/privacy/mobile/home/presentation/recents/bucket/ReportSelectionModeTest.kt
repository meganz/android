package mega.privacy.mobile.home.presentation.recents.bucket

import android.annotation.SuppressLint
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.navigation.contract.state.LocalSelectionModeController
import mega.privacy.android.navigation.contract.state.ReportSelectionMode
import mega.privacy.android.navigation.contract.state.SelectionModeController
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportSelectionModeTest {

    private val composeRule = createComposeRule()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    @Test
    fun `test that callback is invoked with true when in selection mode`() {
        val reportedValues = mutableListOf<Boolean>()

        composeRule.setContent {
            AndroidThemeForPreviews {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalSelectionModeController provides SelectionModeController(
                        isSelectionModeActive = false,
                        onSelectionModeChanged = { reportedValues.add(it) },
                    )
                ) {
                    ReportSelectionMode(isInSelectionMode = true)
                }
            }
        }

        composeRule.waitForIdle()

        assertThat(reportedValues).containsExactly(true)
    }

    @Test
    fun `test that callback is invoked with false when not in selection mode`() {
        val reportedValues = mutableListOf<Boolean>()

        composeRule.setContent {
            AndroidThemeForPreviews {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalSelectionModeController provides SelectionModeController(
                        isSelectionModeActive = false,
                        onSelectionModeChanged = { reportedValues.add(it) },
                    )
                ) {
                    ReportSelectionMode(isInSelectionMode = false)
                }
            }
        }

        composeRule.waitForIdle()

        assertThat(reportedValues).containsExactly(false)
    }

    @Test
    fun `test that callback is invoked with false when composable is disposed`() {
        val reportedValues = mutableListOf<Boolean>()

        @SuppressLint("UnrememberedMutableState")
        @Composable
        fun TestContent() {
            var showReporter by mutableStateOf(true)
            androidx.compose.runtime.CompositionLocalProvider(
                LocalSelectionModeController provides SelectionModeController(
                    isSelectionModeActive = false,
                    onSelectionModeChanged = { reportedValues.add(it) },
                )
            ) {
                if (showReporter) {
                    ReportSelectionMode(isInSelectionMode = true)
                }
                TextButton(
                    onClick = { showReporter = false },
                    modifier = Modifier.testTag("dismiss")
                ) {
                    Text("Dismiss")
                }
            }
        }

        composeRule.setContent {
            AndroidThemeForPreviews {
                TestContent()
            }
        }

        composeRule.waitForIdle()
        assertThat(reportedValues).containsExactly(true)

        composeRule.onNodeWithTag("dismiss").performClick()
        composeRule.waitForIdle()

        assertThat(reportedValues).containsExactly(true, false)
    }
}
