package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import de.palm.composestateevents.triggered
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.mobile.analytics.event.LinkConfirmPasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkConfirmPasswordFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.AlbumLinkSettingsScreenEvent
import mega.privacy.mobile.analytics.event.LinkDiscardChangesCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkDiscardChangesDialogEvent
import mega.privacy.mobile.analytics.event.LinkDiscardChangesDiscardButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkProFeatureSeeNotNowPlanFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkProFeatureSeePlanFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkRemovePasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkResetPasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyAlbumButtonDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyAlbumButtonEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFileButtonDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFileButtonEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFolderButtonEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFileButtonPressedDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFileButtonPressedEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFolderButtonPressedEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSeparateKeyLearnMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSetPasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSetPasswordFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSettingsSaveButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSettingsSaveFailedEvent
import mega.privacy.mobile.analytics.event.LinkSettingsScreenEvent
import mega.privacy.mobile.analytics.event.LinkUpgradeToProFeatureFileDialogEvent
import mega.privacy.mobile.analytics.event.LinkUpgradeToProFeatureFolderDialogEvent
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.Date

@RunWith(AndroidJUnit4::class)
class LinkSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val loaded = LinkSettingsUiState(isLoading = false, accountType = AccountType.PRO_I)

    private val loadedFolder = loaded.copy(isFolder = true)

    private val free = loaded.copy(accountType = AccountType.FREE)

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

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_LEARN_MORE_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that the separate-key toggle is disabled when a password is enabled`() {
        setContent(uiState = loaded.copy(isPasswordEnabled = true, password = "s3cretPass"))

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).assertIsNotEnabled()
    }

    @Test
    fun `test that tapping the separate-key row does nothing when a password is enabled`() {
        var enabled: Boolean? = null
        setContent(
            uiState = loaded.copy(isPasswordEnabled = true, password = "s3cretPass"),
            onSeparateKeyEnabled = { enabled = it },
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_ROW_TAG).performClick()

        assertThat(enabled).isNull()
    }

    @Test
    fun `test that the separate-key toggle is enabled again once the password is turned off`() {
        setContent(uiState = loaded.copy(isPasswordEnabled = false))

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).assertIsEnabled()
    }

    @Test
    fun `test that the password toggle stays enabled when the separate key is on`() {
        setContent(uiState = loaded.copy(isSeparateKeyEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).assertIsEnabled()
    }

    @Test
    fun `test that the separate-key Learn more link still works when a password is enabled`() {
        var clicked = false
        setContent(
            uiState = loaded.copy(isPasswordEnabled = true, password = "s3cretPass"),
            onLearnMore = { clicked = true },
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_LEARN_MORE_TAG, useUnmergedTree = true)
            .performClick()

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
    fun `test that tapping the expiry toggle opens the date picker without a second tap`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        composeRule.onNodeWithText(context.getString(sharedR.string.general_ok_only))
            .assertIsDisplayed()
    }

    @Test
    fun `test that tapping the expiry row opens the date picker without a second tap`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_ROW_TAG).performClick()

        composeRule.onNodeWithText(context.getString(sharedR.string.general_ok_only))
            .assertIsDisplayed()
    }

    @Test
    fun `test that opening the screen with an existing expiry does not open the date picker`() {
        setContent(uiState = loaded.copy(isExpiryEnabled = true, expiryDate = EXPIRY_MILLIS))

        composeRule.onNodeWithText(context.getString(sharedR.string.general_ok_only))
            .assertDoesNotExist()
    }

    @Test
    fun `test that turning the expiry toggle off does not open the date picker`() {
        setContent(uiState = loaded.copy(isExpiryEnabled = true, expiryDate = EXPIRY_MILLIS))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        composeRule.onNodeWithText(context.getString(sharedR.string.general_ok_only))
            .assertDoesNotExist()
    }

    @Test
    fun `test that the seeded expiry date is displayed in the expiry field`() {
        setContent(
            uiState = loaded.copy(isExpiryEnabled = true, expiryDate = EXPIRY_MILLIS)
        )

        composeRule.onNodeWithText(formattedDate(EXPIRY_MILLIS)).assertIsDisplayed()
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
    fun `test that a strong password shows the Strong label`() {
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

    @Test
    fun `test that a very weak password shows the Weak label`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = "pass",
                passwordStrength = PasswordStrength.VERY_WEAK,
            )
        )

        composeRule.onNodeWithText(context.getString(sharedR.string.password_strength_weak))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that a weak password shows the Weak label`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = "pass",
                passwordStrength = PasswordStrength.WEAK,
            )
        )

        composeRule.onNodeWithText(context.getString(sharedR.string.password_strength_weak))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that a medium password shows the Moderate label`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = "pass",
                passwordStrength = PasswordStrength.MEDIUM,
            )
        )

        composeRule.onNodeWithText(context.getString(sharedR.string.password_strength_moderate))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that a good password shows the Strong label`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = "pass",
                passwordStrength = PasswordStrength.GOOD,
            )
        )

        composeRule.onNodeWithText(context.getString(sharedR.string.password_strength_strong))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that no strength helper text is shown for an invalid strength`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = "pass",
                passwordStrength = PasswordStrength.INVALID,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_STRENGTH_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that no strength helper text is shown before a strength is known`() {
        setContent(uiState = loaded.copy(isPasswordEnabled = true, password = "pass"))

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_STRENGTH_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that the Pro badge is shown on the expiry and password rows for a free account`() {
        setContent(uiState = loaded.copy(accountType = AccountType.FREE))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_PRO_BADGE_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_PRO_BADGE_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the Pro badge is hidden on the expiry and password rows for a paid account`() {
        setContent(uiState = loaded.copy(accountType = AccountType.PRO_I))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_PRO_BADGE_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_PRO_BADGE_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that the expiry and password toggles are enabled for a free account`() {
        setContent(uiState = free)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).assertIsEnabled()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).assertIsEnabled()
    }

    @Test
    fun `test that enabling expiry on a free account shows the upgrade dialog without applying it`() {
        var enabled: Boolean? = null
        setContent(uiState = free, onExpiryEnabled = { enabled = it })

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertIsDisplayed()
        assertThat(enabled).isNull()
    }

    @Test
    fun `test that enabling password on a free account shows the upgrade dialog without applying it`() {
        var enabled: Boolean? = null
        setContent(uiState = free, onPasswordEnabled = { enabled = it })

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).performClick()

        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertIsDisplayed()
        assertThat(enabled).isNull()
    }

    @Test
    fun `test that tapping a locked expiry row shows the upgrade dialog for a free account`() {
        setContent(uiState = free)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_ROW_TAG).performClick()

        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that enabling expiry on a free account does not open the date picker`() {
        setContent(uiState = free)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_ok_only))
            .assertDoesNotExist()
    }

    @Test
    fun `test that choosing See plans on the upgrade dialog invokes onUpgrade`() {
        var upgraded = false
        setContent(uiState = free, onUpgrade = { upgraded = true })

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()
        composeRule.onNodeWithText(
            context.getString(sharedR.string.share_link_upgrade_pro_dialog_see_plans)
        ).performClick()

        assertThat(upgraded).isTrue()
    }

    @Test
    fun `test that choosing Not now on the upgrade dialog dismisses it without upgrading`() {
        var upgraded = false
        setContent(uiState = free, onUpgrade = { upgraded = true })

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()
        composeRule.onNodeWithText(
            context.getString(sharedR.string.share_link_upgrade_pro_dialog_not_now)
        ).performClick()

        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertDoesNotExist()
        assertThat(upgraded).isFalse()
    }

    @Test
    fun `test that the upgrade dialog tracks the file display and button events`() {
        setContent(uiState = free)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()
        assertThat(analyticsRule.events).contains(LinkUpgradeToProFeatureFileDialogEvent)

        composeRule.onNodeWithText(
            context.getString(sharedR.string.share_link_upgrade_pro_dialog_see_plans)
        ).performClick()
        assertThat(analyticsRule.events).contains(LinkProFeatureSeePlanFileButtonPressedEvent)
    }

    @Test
    fun `test that the upgrade dialog tracks the folder display event for a folder`() {
        setContent(uiState = free.copy(isFolder = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkUpgradeToProFeatureFolderDialogEvent)
    }

    @Test
    fun `test that Not now tracks the file not-now event`() {
        setContent(uiState = free)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()
        composeRule.onNodeWithText(
            context.getString(sharedR.string.share_link_upgrade_pro_dialog_not_now)
        ).performClick()

        assertThat(analyticsRule.events)
            .contains(LinkProFeatureSeeNotNowPlanFileButtonPressedEvent)
    }

    @Test
    fun `test that a paid account does not see the upgrade dialog when enabling expiry`() {
        var enabled: Boolean? = null
        setContent(uiState = loaded, onExpiryEnabled = { enabled = it })

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertDoesNotExist()
        assertThat(enabled).isTrue()
    }

    @Test
    fun `test that the expiry and password toggles are enabled for a paid account`() {
        setContent(uiState = loaded.copy(accountType = AccountType.PRO_I))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).assertIsEnabled()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).assertIsEnabled()
    }

    @Test
    fun `test that tapping the separate-key toggle tracks the file enabled event`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSendDecryptionKeyFileButtonEnabledEvent)
    }

    @Test
    fun `test that tapping the separate-key toggle tracks the folder enabled event for a folder`() {
        setContent(uiState = loadedFolder)

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSendDecryptionKeyFolderButtonEnabledEvent)
    }

    @Test
    fun `test that tapping the separate-key toggle off tracks the file disabled event`() {
        setContent(uiState = loaded.copy(isSeparateKeyEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSendDecryptionKeyFileButtonDisabledEvent)
    }

    @Test
    fun `test that tapping the expiry toggle tracks the file enabled event`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSetExpiryDateFileButtonPressedEnabledEvent)
    }

    @Test
    fun `test that tapping the expiry toggle tracks the folder enabled event for a folder`() {
        setContent(uiState = loadedFolder)

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSetExpiryDateFolderButtonPressedEnabledEvent)
    }

    @Test
    fun `test that tapping the expiry toggle off tracks the file disabled event`() {
        setContent(uiState = loaded.copy(isExpiryEnabled = true, expiryDate = EXPIRY_MILLIS))

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSetExpiryDateFileButtonPressedDisabledEvent)
    }

    @Test
    fun `test that tapping the password toggle tracks the file set password event`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSetPasswordFileButtonPressedEvent)
    }

    @Test
    fun `test that tapping the password toggle tracks the folder set password event for a folder`() {
        setContent(uiState = loadedFolder)

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSetPasswordFolderButtonPressedEvent)
    }

    @Test
    fun `test that tapping the password toggle off tracks the remove password event when a password was set`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                isPasswordAlreadySet = true,
                initialPassword = PASSWORD,
                password = PASSWORD,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkRemovePasswordFileButtonPressedEvent)
    }

    @Test
    fun `test that tapping the password toggle off does not track the remove password event when no password was set`() {
        setContent(uiState = loaded.copy(isPasswordEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).doesNotContain(LinkRemovePasswordFileButtonPressedEvent)
    }

    @Test
    fun `test that tapping Save tracks the confirm password event when a password is set for the first time`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                password = PASSWORD,
                isSaveEnabled = true,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkConfirmPasswordFileButtonPressedEvent)
        assertThat(analyticsRule.events).doesNotContain(LinkResetPasswordFileButtonPressedEvent)
    }

    @Test
    fun `test that tapping Save tracks the folder confirm password event for a folder`() {
        setContent(
            uiState = loadedFolder.copy(
                isPasswordEnabled = true,
                password = PASSWORD,
                isSaveEnabled = true,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkConfirmPasswordFolderButtonPressedEvent)
    }

    @Test
    fun `test that tapping Save tracks the reset password event when an existing password is changed`() {
        setContent(
            uiState = loaded.copy(
                isPasswordEnabled = true,
                isPasswordAlreadySet = true,
                initialPassword = OLD_PASSWORD,
                password = PASSWORD,
                isSaveEnabled = true,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkResetPasswordFileButtonPressedEvent)
        assertThat(analyticsRule.events).doesNotContain(LinkConfirmPasswordFileButtonPressedEvent)
    }

    @Test
    fun `test that tapping Save does not track a password event when only the expiry date changed`() {
        setContent(
            uiState = loaded.copy(
                isExpiryEnabled = true,
                expiryDate = EXPIRY_MILLIS,
                isSaveEnabled = true,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).performClick()

        assertThat(analyticsRule.events).doesNotContain(LinkConfirmPasswordFileButtonPressedEvent)
        assertThat(analyticsRule.events).doesNotContain(LinkResetPasswordFileButtonPressedEvent)
    }

    @Test
    fun `test that the screen view event is tracked once when the screen is shown`() {
        setContent(uiState = loaded)

        assertThat(analyticsRule.events.filterIsInstance<LinkSettingsScreenEvent>()).hasSize(1)
    }

    @Test
    fun `test that tapping Save tracks the save button event`() {
        setContent(uiState = loaded.copy(isSaveEnabled = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_SAVE_BUTTON_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSettingsSaveButtonPressedEvent)
    }

    @Test
    fun `test that tapping the separate-key Learn more link tracks the learn more event`() {
        setContent(uiState = loaded)

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_LEARN_MORE_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(analyticsRule.events).contains(LinkSeparateKeyLearnMoreButtonPressedEvent)
    }

    @Test
    fun `test that showing the discard dialog tracks the dialog displayed event once`() {
        setContent(uiState = loaded.copy(hasUnsavedChanges = true))

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()
        composeRule.waitForIdle()

        assertThat(analyticsRule.events.filterIsInstance<LinkDiscardChangesDialogEvent>())
            .hasSize(1)
    }

    @Test
    fun `test that the discard dialog displayed event is not tracked until the dialog is shown`() {
        setContent(uiState = loaded.copy(hasUnsavedChanges = true))

        assertThat(analyticsRule.events).doesNotContain(LinkDiscardChangesDialogEvent)
    }

    @Test
    fun `test that discarding changes tracks the discard event`() {
        setContent(uiState = loaded.copy(hasUnsavedChanges = true))

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_dialog_discard_button))
            .performClick()

        assertThat(analyticsRule.events).contains(LinkDiscardChangesDiscardButtonPressedEvent)
    }

    @Test
    fun `test that cancelling the discard dialog tracks the cancel event`() {
        setContent(uiState = loaded.copy(hasUnsavedChanges = true))

        composeRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_dialog_cancel_button))
            .performClick()

        assertThat(analyticsRule.events).contains(LinkDiscardChangesCancelButtonPressedEvent)
    }

    @Test
    fun `test that a triggered error event tracks the save failed event`() {
        setContent(uiState = loaded.copy(errorEvent = triggered))

        assertThat(analyticsRule.events).contains(LinkSettingsSaveFailedEvent)
    }

    @Test
    fun `test that the save failed event is not tracked without an error`() {
        setContent(uiState = loaded)

        assertThat(analyticsRule.events).doesNotContain(LinkSettingsSaveFailedEvent)
    }

    @Test
    fun `test that an album shows the separate key row and neither the expiry nor the password row`() {
        setContent(uiState = LinkSettingsUiState(isLoading = false, isAlbum = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_ROW_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that an album shows no Pro badge on any row`() {
        setContent(
            uiState = LinkSettingsUiState(
                isLoading = false,
                isAlbum = true,
                accountType = AccountType.FREE,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_EXPIRY_PRO_BADGE_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(LINK_SETTINGS_PASSWORD_PRO_BADGE_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that the album separate key toggle stays interactive for a free account`() {
        var enabled: Boolean? = null
        setContent(
            uiState = LinkSettingsUiState(
                isLoading = false,
                isAlbum = true,
                accountType = AccountType.FREE,
            ),
            onSeparateKeyEnabled = { enabled = it },
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(enabled).isTrue()
        composeRule.onNodeWithTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that an album fires the album link settings screen view event and not the node one`() {
        setContent(uiState = LinkSettingsUiState(isLoading = false, isAlbum = true))

        assertThat(analyticsRule.events.count { it == AlbumLinkSettingsScreenEvent }).isEqualTo(1)
        assertThat(analyticsRule.events).doesNotContain(LinkSettingsScreenEvent)
    }

    @Test
    fun `test that a node fires the link settings screen view event and not the album one`() {
        setContent(uiState = loaded)

        assertThat(analyticsRule.events.count { it == LinkSettingsScreenEvent }).isEqualTo(1)
        assertThat(analyticsRule.events).doesNotContain(AlbumLinkSettingsScreenEvent)
    }

    @Test
    fun `test that tapping the separate-key toggle on an album tracks the album enabled event`() {
        setContent(uiState = LinkSettingsUiState(isLoading = false, isAlbum = true))

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSendDecryptionKeyAlbumButtonEnabledEvent)
        assertThat(analyticsRule.events).doesNotContain(LinkSendDecryptionKeyFileButtonEnabledEvent)
    }

    @Test
    fun `test that tapping the separate-key toggle off on an album tracks the album disabled event`() {
        setContent(
            uiState = LinkSettingsUiState(
                isLoading = false,
                isAlbum = true,
                isSeparateKeyEnabled = true,
            )
        )

        composeRule.onNodeWithTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG).performClick()

        assertThat(analyticsRule.events).contains(LinkSendDecryptionKeyAlbumButtonDisabledEvent)
        assertThat(analyticsRule.events).doesNotContain(LinkSendDecryptionKeyFileButtonDisabledEvent)
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
        onUpgrade: () -> Unit = {},
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
                onUpgrade = onUpgrade,
            )
        }
    }

    // Mirrors the screen's own MEDIUM date formatting so the assertion is locale-independent.
    private fun formattedDate(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

    private companion object {
        const val NAVIGATION_ICON = "Navigation Icon"

        // core-ui BaseTextField's editable OutlinedTextField tag; the input field's public
        // testTag is only on the wrapper, so text input must target the inner node.
        const val CORE_UI_TEXT_FIELD_TAG = "base_text_field:outlined_text_field"

        // A fixed, far-future instant used to seed the expiry field.
        const val EXPIRY_MILLIS = 1_800_000_000_000L

        const val PASSWORD = "Str0ngP@ss"
        const val OLD_PASSWORD = "0ldP@ssw0rd"
    }
}
