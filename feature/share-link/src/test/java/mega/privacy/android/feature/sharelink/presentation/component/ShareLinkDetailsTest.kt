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
class ShareLinkDetailsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the card and link value are displayed`() {
        setContent(link = "https://mega.nz/file/abc123")

        composeRule.onNodeWithTag(SHARE_LINK_DETAILS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("https://mega.nz/file/abc123").assertIsDisplayed()
    }

    @Test
    fun `test that tapping the copy icon invokes onCopyLink`() {
        var copied = false
        setContent(onCopyLink = { copied = true })

        composeRule.onNodeWithTag(SHARE_LINK_DETAIL_ROW_COPY_TAG).performClick()

        assertThat(copied).isTrue()
    }

    @Test
    fun `test that the key card is hidden when no key is provided`() {
        setContent()

        composeRule.onNodeWithTag(SHARE_LINK_KEY_DETAILS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the key card and value are displayed when a key is provided`() {
        setContent(key = "decryptionKey")

        composeRule.onNodeWithTag(SHARE_LINK_KEY_DETAILS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("decryptionKey").assertIsDisplayed()
    }

    @Test
    fun `test that tapping the key copy icon invokes onCopyKey`() {
        var copied = false
        setContent(key = "decryptionKey", onCopyKey = { copied = true })

        composeRule.onNodeWithTag(SHARE_LINK_KEY_COPY_TAG).performClick()

        assertThat(copied).isTrue()
    }

    private fun setContent(
        link: String = "https://mega.nz/file/abc123",
        onCopyLink: () -> Unit = {},
        key: String? = null,
        onCopyKey: () -> Unit = {},
    ) {
        composeRule.setContent {
            ShareLinkDetails(
                link = link,
                onCopyLink = onCopyLink,
                key = key,
                onCopyKey = onCopyKey,
            )
        }
    }
}
