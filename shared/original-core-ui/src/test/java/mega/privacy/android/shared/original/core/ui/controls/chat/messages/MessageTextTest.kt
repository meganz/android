package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.activity.ComponentActivity
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostStateOriginal
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.annotation.GraphicsMode

// The native graphics mode measures the real text width, so a tap at the center hits the link.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class MessageTextTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val uriHandler = mock<UriHandler>()
    private lateinit var snackbarHostState: SnackbarHostState
    private var isEdited by mutableStateOf(false)

    @Before
    fun setUp() {
        snackbarHostState = mock {
            on { showSnackbar(any(), anyOrNull(), any()) } doReturn SnackbarResult.Dismissed
        }
    }

    @Test
    fun `test that MessageText opens the link once when the link is clicked`() {
        setContent(onLinkClicked = { it })

        clickLink()
        isEdited = true
        composeRule.waitForIdle()

        verify(uriHandler, times(1)).openUri(LINK)
    }

    @Test
    fun `test that MessageText opens no link when onLinkClicked returns null`() {
        setContent(onLinkClicked = { null })

        clickLink()

        verify(uriHandler, never()).openUri(any())
    }

    @Test
    fun `test that MessageText shows the failure message when the link cannot be opened`() {
        doThrow(IllegalStateException()).whenever(uriHandler).openUri(any())
        setContent(onLinkClicked = { it })

        clickLink()

        val failureMessage =
            composeRule.activity.getString(R.string.chat_click_link_in_message_intent_not_available)
        verifyBlocking(snackbarHostState) { showSnackbar(eq(failureMessage), anyOrNull(), any()) }
    }

    @Test
    fun `test that MessageText shows no snackbar when the link opens`() {
        setContent(onLinkClicked = { it })

        clickLink()

        verify(uriHandler).openUri(LINK)
        verifyBlocking(snackbarHostState, never()) { showSnackbar(any(), anyOrNull(), any()) }
    }

    private fun setContent(onLinkClicked: (String) -> String?) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides uriHandler,
                LocalSnackBarHostStateOriginal provides snackbarHostState,
            ) {
                MessageText(
                    message = LINK,
                    isEdited = isEdited,
                    links = listOf(LINK),
                    interactionEnabled = true,
                    onLinkClicked = onLinkClicked,
                    onLongClick = {},
                )
            }
        }
    }

    private fun clickLink() {
        composeRule.onNodeWithText(LINK, substring = true).performTouchInput { click() }
        composeRule.waitForIdle()
    }

    private companion object {
        const val LINK = "https://mega.nz"
    }
}
