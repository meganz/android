package mega.privacy.android.app.deeplinks.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.deeplinks.model.DeepLinksUIState
import mega.privacy.android.app.presentation.login.LoginNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class DeepLinksDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onNavigate = mock<(List<NavKey>) -> Unit>()
    private val onDismiss = mock<() -> Unit>()

    @Test
    fun `test that dialog shows if navKeys is null`() {
        initComposeTestRule(uiState = DeepLinksUIState(navKeys = null))

        composeTestRule.onNodeWithTag(DEEP_LINK_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that dialog does not show if navKeys is empty and onDismiss is invoked but not onNavigate`() {
        initComposeTestRule(uiState = DeepLinksUIState(navKeys = emptyList()))

        composeTestRule.onNodeWithTag(DEEP_LINK_DIALOG_TEST_TAG).assertIsNotDisplayed()
        verify(onDismiss).invoke()
        verifyNoInteractions(onNavigate)
    }

    @Test
    fun `test that dialog does not show if navKeys is not empty and onDismiss and onNavigate are invoke`() {
        val navKeys = listOf(LoginNavKey())
        initComposeTestRule(uiState = DeepLinksUIState(navKeys = navKeys))

        composeTestRule.onNodeWithTag(DEEP_LINK_DIALOG_TEST_TAG).assertIsNotDisplayed()
        verify(onDismiss).invoke()
        verify(onNavigate).invoke(navKeys)
    }

    private fun initComposeTestRule(uiState: DeepLinksUIState) {
        composeTestRule.setContent {
            DeepLinksDialog(
                uiState = uiState,
                onNavigate = onNavigate,
                onDismiss = onDismiss,
            )
        }
    }
}