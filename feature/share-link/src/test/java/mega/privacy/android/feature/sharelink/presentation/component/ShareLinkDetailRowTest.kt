package mega.privacy.android.feature.sharelink.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareLinkDetailRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the label and value are displayed`() {
        setContent(label = "Link", value = "https://mega.nz/file/abc123")

        composeRule.onNodeWithText("Link").assertIsDisplayed()
        composeRule.onNodeWithText("https://mega.nz/file/abc123").assertIsDisplayed()
    }

    @Test
    fun `test that tapping the copy icon invokes onCopy`() {
        var copied = false
        setContent(onCopy = { copied = true })

        composeRule.onNodeWithTag(SHARE_LINK_DETAIL_ROW_COPY_TAG).performClick()

        assertThat(copied).isTrue()
    }

    private fun setContent(
        label: String = "Link",
        value: String = "https://mega.nz/file/abc123",
        onCopy: () -> Unit = {},
    ) {
        composeRule.setContent {
            ShareLinkDetailRow(
                label = label,
                value = value,
                onCopy = onCopy,
            )
        }
    }
}
