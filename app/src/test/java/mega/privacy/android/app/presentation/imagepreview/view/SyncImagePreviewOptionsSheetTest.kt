package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class SyncImagePreviewOptionsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val optionsSheetState = ImagePreviewOptionsSheetState()
    private lateinit var modalSheetState: ModalBottomSheetState

    @Test
    fun `test that show opens the options sheet`() {
        setContent()

        composeTestRule.runOnUiThread { optionsSheetState.show() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SHEET_CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that show opens the options sheet when an earlier opening animation was interrupted`() {
        setContent()
        interruptTheOpeningAnimation()
        assertThat(modalSheetState.isVisible).isFalse()

        composeTestRule.runOnUiThread { optionsSheetState.show() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SHEET_CONTENT_TAG).assertIsDisplayed()
    }

    /**
     * Dismisses the sheet through the scrim while it is still animating open. The scrim hides the
     * sheet through the same state, which preempts the in flight opening animation and cancels it.
     */
    private fun interruptTheOpeningAnimation() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread { optionsSheetState.show() }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeUntil {
            modalSheetState.targetValue != ModalBottomSheetValue.Hidden
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        assertThat(modalSheetState.isVisible).isFalse()

        composeTestRule.onNode(hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }

    /**
     * The sheet layout stays composed for the whole test so that its drag anchors are in place
     * before the first request, which is what gives [ModalBottomSheetState.show] an animation to
     * run rather than settling immediately.
     */
    private fun setContent() {
        composeTestRule.setContent {
            modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = false,
            )

            SyncImagePreviewOptionsSheet(
                optionsSheetState = optionsSheetState,
                modalSheetState = modalSheetState,
            )

            ModalBottomSheetLayout(
                sheetState = modalSheetState,
                sheetContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag(SHEET_CONTENT_TAG)
                    )
                },
                content = { Box(modifier = Modifier.fillMaxSize()) },
            )
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val SHEET_CONTENT_TAG = "options_sheet_content"
    }
}
