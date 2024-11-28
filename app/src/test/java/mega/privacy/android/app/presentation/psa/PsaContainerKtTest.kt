package mega.privacy.android.app.presentation.psa

import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.psa.view.PsaInfoViewTag
import mega.privacy.android.app.presentation.psa.view.PsaViewTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PsaContainerKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that no PSA displays the content only`() {
        val contentTag = "Content tag"
        composeTestRule.setContent {
            PsaContainerContent(
                state = PsaState.NoPsa,
                content = { Text("This is the content", Modifier.testTag(contentTag)) },
                markAsSeen = {},
                navigateToPsaPage = {}
            )
        }

        composeTestRule.onNodeWithTag(contentTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaViewTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertDoesNotExist()
    }

    @Test
    fun `test that info psa displays the correct view`() {
        val contentTag = "Content tag"
        composeTestRule.setContent {
            PsaContainerContent(
                state = PsaState.InfoPsa(
                    id = 0,
                    title = "title",
                    text = "test",
                    imageUrl = null,
                ),
                content = { Text("This is the content", Modifier.testTag(contentTag)) },
                markAsSeen = {},
                navigateToPsaPage = {}
            )
        }

        composeTestRule.onNodeWithTag(contentTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaViewTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertIsDisplayed()
    }

    @Test
    fun `test that standard psa displays the correct view`() {
        val contentTag = "Content tag"
        composeTestRule.setContent {
            PsaContainerContent(
                state = PsaState.StandardPsa(
                    id = 0,
                    title = "title",
                    text = "test",
                    imageUrl = null,
                    positiveText = "positiveText",
                    positiveLink = "positiveLink"
                ),
                content = { Text("This is the content", Modifier.testTag(contentTag)) },
                markAsSeen = {},
                navigateToPsaPage = {}
            )
        }

        composeTestRule.onNodeWithTag(contentTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaViewTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertDoesNotExist()
    }
}