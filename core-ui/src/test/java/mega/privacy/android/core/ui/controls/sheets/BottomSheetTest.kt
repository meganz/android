package mega.privacy.android.core.ui.controls.sheets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class BottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that when view is set bottom sheet is not displayed to the user`() {
        val message = "message to test"
        val button = "show-bottom-sheet"
        val sheetState = ModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            isSkipHalfExpanded = false,
        )
        composeTestRule.setContent {
            val coroutineScope = rememberCoroutineScope()
            BottomSheet(
                modalSheetState = sheetState,
                sheetHeader = { Text(text = "sample") },
                sheetBody = {
                    LazyColumn {
                        items(100) {
                            MenuActionListTile(text = "title $it")
                        }
                    }
                },
            ) {
                Scaffold {
                    Text(modifier = Modifier.padding(it), text = message)
                    Button(
                        modifier = Modifier.testTag(button),
                        onClick = {
                            coroutineScope.launch {
                                sheetState.show()
                            }
                        },
                    ) {
                        Text(text = "Show modal sheet")
                    }
                }
            }
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithText("sample").assertIsNotDisplayed()
    }

    @Test
    fun `test that when user clicks on button to show bottom sheet bottom sheet is shown to user`() {
        val message = "message to test"
        val button = "show-bottom-sheet"
        val sheetState = ModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            isSkipHalfExpanded = false,
        )
        composeTestRule.setContent {
            val coroutineScope = rememberCoroutineScope()
            BottomSheet(
                modalSheetState = sheetState,
                sheetHeader = { Text(text = "sample") },
                sheetBody = {
                    LazyColumn {
                        items(100) {
                            MenuActionListTile(text = "title $it")
                        }
                    }
                },
            ) {
                Scaffold {
                    Text(modifier = Modifier.padding(it), text = message)
                    Button(
                        modifier = Modifier.testTag(button),
                        onClick = {
                            coroutineScope.launch {
                                sheetState.show()
                            }
                        },
                    ) {
                        Text(text = "Show modal sheet")
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("sample").assertIsNotDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithTag(button).assertIsDisplayed()
        composeTestRule.onNodeWithTag(button).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("sample").assertIsDisplayed()
    }
}