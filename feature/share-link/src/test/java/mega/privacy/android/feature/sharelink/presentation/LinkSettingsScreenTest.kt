package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class LinkSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val loaded = LinkSettingsUiState(isLoading = false)

    @Test
    fun `test that the loading placeholder is displayed while loading`() {
        setContent(uiState = LinkSettingsUiState(isLoading = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the separate-key, expiry and password rows and Save button are displayed once loaded`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the separate-key toggle invokes onSeparateKeyEnabled`() {
        var enabled: Boolean? = null
        setContent(uiState = loaded, onSeparateKeyEnabled = { enabled = it })

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(enabled).isTrue()
    }

    @Test
    fun `test that tapping the separate-key Learn more link invokes onLearnMore`() {
        var clicked = false
        setContent(uiState = loaded, onLearnMore = { clicked = true })

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_LEARN_MORE_TAG).performClick()

        assertThat(clicked).isTrue()
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

    @Test
    fun `test that closing without unsaved changes invokes onBack`() {
        var backed = false
        setContent(uiState = loaded.copy(hasUnsavedChanges = false), onBack = { backed = true })

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()

        assertThat(backed).isTrue()
    }

    @Test
    fun `test that closing with unsaved changes shows the discard dialog without going back`() {
        var backed = false
        setContent(uiState = loaded.copy(hasUnsavedChanges = true), onBack = { backed = true })

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()

        composeRule.onNodeWithText(
            context.getString(sharedR.string.general_dialog_title_discard_changes)
        ).assertIsDisplayed()
        assertThat(backed).isFalse()
    }

    @Test
    fun `test that discarding changes invokes onBack`() {
        var backed = false
        setContent(uiState = loaded.copy(hasUnsavedChanges = true), onBack = { backed = true })

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_dialog_discard_button))
            .performClick()

        assertThat(backed).isTrue()
    }

    @Test
    fun `test that cancelling the discard dialog keeps the user on the screen`() {
        var backed = false
        setContent(uiState = loaded.copy(hasUnsavedChanges = true), onBack = { backed = true })

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_dialog_cancel_button))
            .performClick()

        assertThat(backed).isFalse()
    }

    @Test
    fun `test that the expiry date field is hidden when the expiry toggle is off`() {
        setContent(uiState = loaded.copy(isExpiryEnabled = false))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the expiry date field is revealed when the expiry toggle is on`() {
        setContent(uiState = loaded.copy(isExpiryEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_FIELD_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the expiry date field opens the date picker`() {
        setContent(uiState = loaded.copy(isExpiryEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_FIELD_TAG).performClick()

        composeRule.onNodeWithText(context.getString(sharedR.string.general_ok_only))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the seeded expiry date is displayed in the expiry field`() {
        setContent(
            uiState = loaded.copy(isExpiryEnabled = true, expiryDate = EXPIRY_MILLIS)
        )

        composeRule.onNodeWithText(formattedUtcDate(EXPIRY_MILLIS)).assertIsDisplayed()
    }

    @Test
    fun `test that the password field is hidden when the password toggle is off`() {
        setContent(uiState = loaded.copy(isPasswordEnabled = false))

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the password field is revealed when the password toggle is on`() {
        setContent(uiState = loaded.copy(isPasswordEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_FIELD_TAG).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that typing in the password field invokes onPasswordChanged`() {
        var typed: String? = null
        setContent(uiState = loaded.copy(isPasswordEnabled = true), onPasswordChanged = { typed = it })

        composeRule.onNodeWithTag(CORE_UI_TEXT_FIELD_TAG).performTextInput("a")

        assertThat(typed).isEqualTo("a")
    }

    @Test
    fun `test that the strength helper text is displayed for a strong password`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = "Str0ngP@ss",
                passwordStrength = PasswordStrength.STRONG,
            )
        )

        composeRule.onNodeWithText(
            context.getString(sharedR.string.password_strength_strong)
        ).performScrollTo().assertIsDisplayed()
    }

    private fun setContent(
        uiState: LinkSettingsUiState,
        onBack: () -> Unit = {},
        onSeparateKeyEnabled: (Boolean) -> Unit = {},
        onLearnMore: () -> Unit = {},
        onExpiryEnabled: (Boolean) -> Unit = {},
        onExpiryDateChanged: (Long) -> Unit = {},
        onPasswordEnabled: (Boolean) -> Unit = {},
        onPasswordChanged: (String) -> Unit = {},
        onSave: () -> Unit = {},
    ) {
        composeRule.setContent {
            LinkSettingsScreen(
                uiState = uiState,
                onBack = onBack,
                onSeparateKeyEnabled = onSeparateKeyEnabled,
                onLearnMore = onLearnMore,
                onExpiryEnabled = onExpiryEnabled,
                onExpiryDateChanged = onExpiryDateChanged,
                onPasswordEnabled = onPasswordEnabled,
                onPasswordChanged = onPasswordChanged,
                onSave = onSave,
            )
        }
    }

    // Mirrors the screen's own UTC MEDIUM formatting so the assertion is locale-independent.
    private fun formattedUtcDate(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    private companion object {
        const val NAVIGATION_ICON = "Navigation Icon"

        // core-ui BaseTextField's editable OutlinedTextField tag; the input field's public
        // testTag is only on the wrapper, so text input must target the inner node.
        const val CORE_UI_TEXT_FIELD_TAG = "base_text_field:outlined_text_field"

        // A fixed, far-future instant used to seed the expiry field.
        const val EXPIRY_MILLIS = 1_800_000_000_000L
    }
}
