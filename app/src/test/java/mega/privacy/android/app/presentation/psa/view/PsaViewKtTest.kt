package mega.privacy.android.app.presentation.psa.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class PsaViewKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that psa view contains required fields`() {
        composeTestRule.setContent {
            PsaView(
                title = "title",
                text = "text",
                imageUrl = "imageUrl",
                positiveText = "positiveText",
                onPositiveTapped = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag(PsaTitleTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaBodyTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaImageViewTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaPositiveButtonTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaDismissButtonTag).assertIsDisplayed()
    }

    @Test
    fun `test that info psa view contains required fields`() {
        composeTestRule.setContent {
            InfoPsaView(
                title = "title",
                text = "text",
                imageUrl = "imageUrl",
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag(PsaTitleTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaBodyTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaImageViewTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaDismissButtonTag).assertIsDisplayed()
    }

    @Test
    fun `test that psa view buttons call the correct functions`() {
        val onPositiveTapped = mock<() -> Unit>()
        val onDismiss = mock<() -> Unit>()
        composeTestRule.setContent {
            PsaView(
                title = "title",
                text = "text",
                imageUrl = "imageUrl",
                positiveText = "positiveText",
                onPositiveTapped = onPositiveTapped,
                onDismiss = onDismiss
            )
        }
        verifyNoInteractions(onPositiveTapped)
        verifyNoInteractions(onDismiss)
        composeTestRule.onNodeWithTag(PsaPositiveButtonTag).performClick()
        verify(onPositiveTapped).invoke()
        composeTestRule.onNodeWithTag(PsaDismissButtonTag).performClick()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that info psa view buttons call the correct functions`() {
        val onDismiss = mock<() -> Unit>()
        composeTestRule.setContent {
            InfoPsaView(
                title = "title",
                text = "text",
                imageUrl = "imageUrl",
                onDismiss = onDismiss
            )
        }
        verifyNoInteractions(onDismiss)
        composeTestRule.onNodeWithTag(PsaDismissButtonTag).performClick()
        verify(onDismiss).invoke()
    }
}