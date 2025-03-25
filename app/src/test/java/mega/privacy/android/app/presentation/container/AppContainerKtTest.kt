package mega.privacy.android.app.presentation.container

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContainerKtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that content is shown if no container`() {
        val expectedText = "This is the content"
        composeTestRule.setContent {
            AppContainer(
                containers = emptyList()
            ) {
                Text(expectedText)
            }
        }

        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun `test that container is shown if included`() {
        val expectedText = "This is the content"
        val expectedContainerText = "Expected container text"
        composeTestRule.setContent {
            AppContainer(
                containers = listOf(
                    { content ->
                        Column {
                            Text(expectedContainerText)
                            content()
                        }
                    }
                )
            ) {
                Text(expectedText)
            }
        }

        composeTestRule.onNodeWithText(expectedContainerText).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun `test that containers are applied in reverse order`() {
        val notExpectedText = "This is the content"
        val expectedContainerText = "Expected container text"

        composeTestRule.setContent {
            AppContainer(
                containers = listOf(
                    { _ ->
                        Text(notExpectedText)
                    },
                    { _ ->
                        Text(expectedContainerText)
                    },
                )
            ) {
                Text(notExpectedText)
            }
        }

        composeTestRule.onNodeWithText(expectedContainerText).assertIsDisplayed()
        composeTestRule.onNodeWithText(notExpectedText).assertIsNotDisplayed()
    }
}