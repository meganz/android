package mega.privacy.android.app.presentation.psa

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.psa.view.PsaInfoViewTag
import mega.privacy.android.app.presentation.psa.view.PsaViewTag
import mega.privacy.android.app.presentation.psa.view.WebPsaTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PsaContainerKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that no PSA displays nothing`() {
        composeTestRule.setContent {
            PsaStateView(
                state = PsaState.NoPsa,
                markAsSeen = {},
                navigateToPsaPage = {},
                innerModifier = { it },
                containerModifier = Modifier,
            )
        }

        composeTestRule.onNodeWithTag(WebPsaTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaViewTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertDoesNotExist()
    }

    @Test
    fun `test that info psa displays the correct view`() {
        composeTestRule.setContent {
            PsaStateView(
                state = PsaState.InfoPsa(
                    id = 0,
                    title = "title",
                    text = "test",
                    imageUrl = null,
                ),
                markAsSeen = {},
                navigateToPsaPage = {},
                innerModifier = { it },
                containerModifier = Modifier,
            )
        }

        composeTestRule.onNodeWithTag(WebPsaTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaViewTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertIsDisplayed()
    }

    @Test
    fun `test that standard psa displays the correct view`() {
        composeTestRule.setContent {
            PsaStateView(
                state = PsaState.StandardPsa(
                    id = 0,
                    title = "title",
                    text = "test",
                    imageUrl = null,
                    positiveText = "positiveText",
                    positiveLink = "positiveLink"
                ),
                markAsSeen = {},
                navigateToPsaPage = {},
                innerModifier = { it },
                containerModifier = Modifier,
            )
        }

        composeTestRule.onNodeWithTag(WebPsaTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaViewTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertDoesNotExist()
    }

    @Test
    fun `test that web psa displays the correct view`() {
        composeTestRule.setContent {
            PsaStateView(
                state = PsaState.WebPsa(
                    id = 0,
                    url = "https://www.mega.nz"
                ),
                markAsSeen = {},
                navigateToPsaPage = {},
                innerModifier = { it },
                containerModifier = Modifier,
            )
        }

        composeTestRule.onNodeWithTag(PsaViewTag).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PsaInfoViewTag).assertDoesNotExist()
    }
}