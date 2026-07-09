package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val loaded = LinkSettingsUiState(isLoading = false)

    @Test
    fun `test that the loading placeholder is displayed while loading`() {
        setContent(uiState = LinkSettingsUiState(isLoading = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the expiry and password rows and Save button are displayed once loaded`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the expiry toggle invokes onExpiryEnabled`() {
        var enabled: Boolean? = null
        setContent(uiState = loaded, onExpiryEnabled = { enabled = it })

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        assertThat(enabled).isTrue()
    }

    @Test
    fun `test that tapping the password toggle invokes onPasswordEnabled`() {
        var enabled: Boolean? = null
        setContent(uiState = loaded, onPasswordEnabled = { enabled = it })

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).performClick()

        assertThat(enabled).isTrue()
    }

    @Test
    fun `test that the Save button is disabled when the selection is not saveable`() {
        setContent(uiState = loaded.copy(isSaveEnabled = false))

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun `test that tapping Save invokes onSave when enabled`() {
        var saved = false
        setContent(uiState = loaded.copy(isSaveEnabled = true), onSave = { saved = true })

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).performClick()

        assertThat(saved).isTrue()
    }

    private fun setContent(
        uiState: LinkSettingsUiState,
        onBack: () -> Unit = {},
        onExpiryEnabled: (Boolean) -> Unit = {},
        onPasswordEnabled: (Boolean) -> Unit = {},
        onSave: () -> Unit = {},
    ) {
        composeRule.setContent {
            LinkSettingsScreen(
                uiState = uiState,
                onBack = onBack,
                onExpiryEnabled = onExpiryEnabled,
                onPasswordEnabled = onPasswordEnabled,
                onSave = onSave,
            )
        }
    }
}
