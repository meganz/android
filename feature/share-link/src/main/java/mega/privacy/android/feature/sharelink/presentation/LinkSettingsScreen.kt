package mega.privacy.android.feature.sharelink.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.Button
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Revamped Link settings editor screen.
 *
 * @param uiState The current [LinkSettingsUiState].
 * @param onBack Invoked when the Close action is tapped.
 * @param onExpiryEnabled Invoked when the "Set expiry date" toggle changes.
 * @param onPasswordEnabled Invoked when the "Set password" toggle changes.
 * @param onPasswordChanged Invoked when the revealed password field text changes.
 * @param onSave Invoked when the bottom "Save" button is tapped.
 * @param modifier Modifier for the scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSettingsScreen(
    uiState: LinkSettingsUiState,
    onBack: () -> Unit,
    onExpiryEnabled: (Boolean) -> Unit,
    onPasswordEnabled: (Boolean) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val onCloseRequest = {
        if (uiState.hasUnsavedChanges) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = uiState.hasUnsavedChanges) { showDiscardDialog = true }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(LINK_SETTINGS_APP_BAR_TAG),
                title = stringResource(sharedR.string.share_link_settings_title),
                navigationType = AppBarNavigationType.Close(onCloseRequest),
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                AnchoredButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    buttonGroup = listOf {
                        Button.PrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(LINK_SETTINGS_SAVE_BUTTON_TAG),
                            text = stringResource(sharedR.string.general_action_save),
                            onClick = onSave,
                            enabled = uiState.isSaveEnabled,
                            isLoading = uiState.isSaving,
                        )
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (uiState.isLoading) {
                LinkSettingsLoading()
            } else {
                LinkSettingsContent(
                    uiState = uiState,
                    onExpiryEnabled = onExpiryEnabled,
                    onPasswordEnabled = onPasswordEnabled,
                    onPasswordChanged = onPasswordChanged,
                )
            }
        }
    }

    if (showDiscardDialog) {
        BasicDialog(
            modifier = Modifier.testTag(LINK_SETTINGS_DISCARD_DIALOG_TAG),
            title = stringResource(sharedR.string.general_dialog_title_discard_changes),
            description = stringResource(sharedR.string.general_dialog_discard_changes_message),
            positiveButtonText = stringResource(sharedR.string.general_dialog_discard_button),
            onPositiveButtonClicked = {
                showDiscardDialog = false
                onBack()
            },
            negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
            onNegativeButtonClicked = { showDiscardDialog = false },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun LinkSettingsContent(
    uiState: LinkSettingsUiState,
    onExpiryEnabled: (Boolean) -> Unit,
    onPasswordEnabled: (Boolean) -> Unit,
    onPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FlexibleLineListItem(
            modifier = Modifier.testTag(LINK_SETTINGS_EXPIRY_ROW_TAG),
            title = stringResource(sharedR.string.share_link_set_expiry_date),
            subtitle = stringResource(sharedR.string.share_link_expiry_subtitle),
            enableClick = true,
            onClickListener = { onExpiryEnabled(!uiState.isExpiryEnabled) },
            trailingElement = {
                Toggle(
                    modifier = Modifier.testTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG),
                    isChecked = uiState.isExpiryEnabled,
                    onCheckedChange = onExpiryEnabled,
                )
            },
        )
        FlexibleLineListItem(
            modifier = Modifier.testTag(LINK_SETTINGS_PASSWORD_ROW_TAG),
            title = stringResource(sharedR.string.share_link_set_password),
            subtitle = stringResource(sharedR.string.share_link_password_subtitle),
            enableClick = true,
            onClickListener = { onPasswordEnabled(!uiState.isPasswordEnabled) },
            trailingElement = {
                Toggle(
                    modifier = Modifier.testTag(LINK_SETTINGS_PASSWORD_TOGGLE_TAG),
                    isChecked = uiState.isPasswordEnabled,
                    onCheckedChange = onPasswordEnabled,
                )
            },
        )
        AnimatedVisibility(visible = uiState.isPasswordEnabled) {
            val strengthLabel = uiState.passwordStrength
                ?.strengthLabelRes()
                ?.let { stringResource(it) }
            PasswordTextInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(LINK_SETTINGS_PASSWORD_FIELD_TAG),
                label = null,
                placeholder = stringResource(sharedR.string.password_placeholder),
                text = uiState.password.orEmpty(),
                showClearIcon = true,
                successText = strengthLabel.takeIf { uiState.passwordStrength.isAcceptable() },
                warningText = strengthLabel.takeIf { uiState.passwordStrength == PasswordStrength.WEAK },
                errorText = strengthLabel.takeIf { uiState.passwordStrength == PasswordStrength.VERY_WEAK },
                onValueChanged = onPasswordChanged,
            )
        }
    }
}

@StringRes
private fun PasswordStrength.strengthLabelRes(): Int? = when (this) {
    PasswordStrength.VERY_WEAK -> sharedR.string.password_strength_very_weak
    PasswordStrength.WEAK -> sharedR.string.password_strength_weak
    PasswordStrength.MEDIUM -> sharedR.string.password_strength_medium
    PasswordStrength.GOOD -> sharedR.string.password_strength_good
    PasswordStrength.STRONG -> sharedR.string.password_strength_strong
    PasswordStrength.INVALID -> null
}

private fun PasswordStrength?.isAcceptable() =
    this == PasswordStrength.MEDIUM || this == PasswordStrength.GOOD || this == PasswordStrength.STRONG

@Composable
private fun LinkSettingsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(LINK_SETTINGS_LOADING_TAG)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(20.dp)
                        .shimmerEffect(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

private val previewData = LinkSettingsUiState(isLoading = false)

@CombinedThemePreviews
@Composable
private fun LinkSettingsScreenPreview() {
    AndroidThemeForPreviews {
        LinkSettingsScreen(
            uiState = previewData,
            onBack = {},
            onExpiryEnabled = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LinkSettingsScreenDirtyPreview() {
    AndroidThemeForPreviews {
        LinkSettingsScreen(
            uiState = previewData.copy(isExpiryEnabled = true, isSaveEnabled = true),
            onBack = {},
            onExpiryEnabled = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LinkSettingsScreenPasswordPreview(
    @PreviewParameter(PasswordStrengthPreviewProvider::class) strength: PasswordStrength,
) {
    AndroidThemeForPreviews {
        LinkSettingsScreen(
            uiState = previewData.copy(
                isPasswordEnabled = true,
                password = "Str0ngP@ss",
                passwordStrength = strength,
                isSaveEnabled = true,
            ),
            onBack = {},
            onExpiryEnabled = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
        )
    }
}

private class PasswordStrengthPreviewProvider : PreviewParameterProvider<PasswordStrength> {
    override val values = sequenceOf(
        PasswordStrength.VERY_WEAK,
        PasswordStrength.WEAK,
        PasswordStrength.MEDIUM,
        PasswordStrength.GOOD,
        PasswordStrength.STRONG,
    )
}

@CombinedThemePreviews
@Composable
private fun LinkSettingsScreenLoadingPreview() {
    AndroidThemeForPreviews {
        LinkSettingsScreen(
            uiState = LinkSettingsUiState(isLoading = true),
            onBack = {},
            onExpiryEnabled = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
        )
    }
}

internal const val LINK_SETTINGS_APP_BAR_TAG = "link_settings_screen:app_bar"
internal const val LINK_SETTINGS_SAVE_BUTTON_TAG = "link_settings_screen:button_save"
internal const val LINK_SETTINGS_EXPIRY_ROW_TAG = "link_settings_screen:row_expiry"
internal const val LINK_SETTINGS_EXPIRY_TOGGLE_TAG = "link_settings_screen:toggle_expiry"
internal const val LINK_SETTINGS_PASSWORD_ROW_TAG = "link_settings_screen:row_password"
internal const val LINK_SETTINGS_PASSWORD_TOGGLE_TAG = "link_settings_screen:toggle_password"
internal const val LINK_SETTINGS_PASSWORD_FIELD_TAG = "link_settings_screen:field_password"
internal const val LINK_SETTINGS_LOADING_TAG = "link_settings_screen:loading"
internal const val LINK_SETTINGS_DISCARD_DIALOG_TAG = "link_settings_screen:discard_dialog"
