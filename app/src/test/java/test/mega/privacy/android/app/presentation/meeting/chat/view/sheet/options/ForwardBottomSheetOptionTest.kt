package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet.options

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.ForwardBottomSheetOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ForwardBottomSheetOptionTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that forward bottom sheet option is displayed correctly`() {
        val click = mock<() -> Unit>()

        with(composeRule) {
            setContent {
                ForwardBottomSheetOption(onClick = click)
            }
            onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG).assertExists()
            onNodeWithText(R.string.forward_menu_item).assertExists()
            onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG).performClick()
            verify(click).invoke()
        }
    }
}