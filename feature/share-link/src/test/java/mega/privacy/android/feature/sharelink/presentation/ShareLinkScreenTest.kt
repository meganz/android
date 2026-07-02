package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareLinkScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val data = ShareLinkUiState.Data(
        handles = listOf(1L),
        nodeName = "Presentation.pdf",
        isFolder = false,
        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
        sizeInBytes = 10L * 1024 * 1024,
        modificationTime = 1_749_000_000L,
        link = "https://mega.nz/file/abc123#decryptionKey",
        linkWithoutKey = "https://mega.nz/file/abc123",
        key = "decryptionKey",
        accountType = null,
    )

    @Test
    fun `test that the node header, link access banner and link field are displayed in the Data state`() {
        setContent(uiState = data)

        composeRule.onNodeWithText("Presentation.pdf").assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_ACCESS_BANNER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_LINK_FIELD_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_SHARE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the loading placeholder is displayed in the Loading state`() {
        setContent(uiState = ShareLinkUiState.Loading)

        composeRule.onNodeWithTag(SHARE_LINK_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the error message is displayed in the Error state`() {
        setContent(uiState = ShareLinkUiState.Error)

        composeRule.onNodeWithTag(SHARE_LINK_ERROR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the share button invokes onShareLink`() {
        var shared = false
        setContent(uiState = data, onShareLink = { shared = true })

        composeRule.onNodeWithTag(SHARE_LINK_SHARE_BUTTON_TAG).performClick()

        assertThat(shared).isTrue()
    }

    @Test
    fun `test that tapping the copy icon invokes onCopyLink`() {
        var copied = false
        setContent(uiState = data, onCopyLink = { copied = true })

        composeRule.onNodeWithContentDescription(context.getString(sharedR.string.general_copy))
            .performClick()

        assertThat(copied).isTrue()
    }

    @Test
    fun `test that tapping the copy icon copies the link to the clipboard`() {
        val clipboard = FakeClipboardManager()
        setContent(uiState = data, clipboardManager = clipboard)

        composeRule.onNodeWithContentDescription(context.getString(sharedR.string.general_copy))
            .performClick()

        assertThat(clipboard.getText()?.text).isEqualTo(data.link)
    }

    private fun setContent(
        uiState: ShareLinkUiState,
        onBack: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onShareLink: () -> Unit = {},
        onCopyLink: () -> Unit = {},
        clipboardManager: ClipboardManager = FakeClipboardManager(),
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                AndroidThemeForPreviews {
                    ShareLinkScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onOpenSettings = onOpenSettings,
                        onShareLink = onShareLink,
                        onCopyLink = onCopyLink,
                    )
                }
            }
        }
    }

    private class FakeClipboardManager : ClipboardManager {
        private var text: AnnotatedString? = null

        override fun setText(annotatedString: AnnotatedString) {
            text = annotatedString
        }

        override fun getText(): AnnotatedString? = text
    }
}
