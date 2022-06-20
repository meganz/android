@file:OptIn(ExperimentalCoroutinesApi::class)

package test.mega.privacy.android.app.presentation.settings.reportissue.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.view.DiscardReportDialog
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiscardReportDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_that_selecting_discard_calls_the_correct_function() {
        val onDiscard = mock<() -> Unit>()
        composeTestRule.setContent {
            DiscardReportDialog(onDiscardCancelled = {}, onDiscard = onDiscard)
        }

        composeTestRule.onNodeWithText(
                fromId(R.string.settings_help_report_issue_discard_button),
                ignoreCase = true
        ).performClick()

        verify(onDiscard).invoke()
    }

    @Test
    fun test_that_cancelling_discard_calls_the_correct_function() {
        val onDiscardCancelled = mock<() -> Unit>()
        composeTestRule.setContent {
            DiscardReportDialog(onDiscardCancelled = onDiscardCancelled, onDiscard = {})
        }

        composeTestRule.onNodeWithText(
                fromId(R.string.general_cancel),
                ignoreCase = true
        ).performClick()

        verify(onDiscardCancelled).invoke()
    }

    @Ignore("dismissOnClickOutside cannot currently be tested via compose rule")
    @Test
    fun test_that_dismissing_the_dialog_calls_cancel_navigation_function() = runTest {
        val onDismiss = mock<() -> Unit>()
        val onButtonPressed = mock<() -> Unit>()
        composeTestRule.setContent {
            Scaffold(topBar = {
                TopAppBar {
                    Text(text = "This test does not work")
                }
            }) { paddingValues ->
                DiscardReportDialog(onDiscardCancelled = onDismiss, onDiscard = {})
                Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onButtonPressed) {
                        Text(text = "test")
                    }
                }
            }
        }

        composeTestRule.onNode(isDialog()).assertExists()

        composeTestRule.onNodeWithText("test", ignoreCase = true).performClick()

        verify(onButtonPressed).invoke()

        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        verify(onDismiss).invoke()

    }
}