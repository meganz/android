package mega.privacy.android.feature.sharelink.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.triggered
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.datepicker.MegaDatePickerDialog
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.inputfields.HelpTextError
import mega.android.core.ui.components.inputfields.HelpTextSuccess
import mega.android.core.ui.components.inputfields.HelpTextWarning
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.ReadOnlyTextInputField
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.model.Button
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AlbumLinkSettingsScreenEvent
import mega.privacy.mobile.analytics.event.LinkConfirmPasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkConfirmPasswordFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkDiscardChangesCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkDiscardChangesDialogEvent
import mega.privacy.mobile.analytics.event.LinkDiscardChangesDiscardButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkProFeatureSeeNotNowPlanFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkProFeatureSeeNotNowPlanFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkProFeatureSeePlanFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkProFeatureSeePlanFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkRemovePasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkRemovePasswordFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkResetPasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkResetPasswordFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyAlbumButtonDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyAlbumButtonEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFileButtonDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFileButtonEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFolderButtonDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSendDecryptionKeyFolderButtonEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSeparateKeyLearnMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFileButtonPressedDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFileButtonPressedEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFolderButtonPressedDisabledEvent
import mega.privacy.mobile.analytics.event.LinkSetExpiryDateFolderButtonPressedEnabledEvent
import mega.privacy.mobile.analytics.event.LinkSetPasswordFileButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSetPasswordFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSettingsSaveButtonPressedEvent
import mega.privacy.mobile.analytics.event.LinkSettingsSaveFailedEvent
import mega.privacy.mobile.analytics.event.LinkSettingsScreenEvent
import mega.privacy.mobile.analytics.event.LinkUpgradeToProFeatureFileDialogEvent
import mega.privacy.mobile.analytics.event.LinkUpgradeToProFeatureFolderDialogEvent
import java.util.Calendar

/**
 * Revamped Link settings editor screen.
 *
 * @param uiState The current [LinkSettingsUiState].
 * @param onBack Invoked when the Close action is tapped.
 * @param onSeparateKeyEnabled Invoked when the "Separate link and key" toggle changes.
 * @param onLearnMore Invoked when the "Learn more" link under the separate-key row is tapped.
 * @param onExpiryEnabled Invoked when the "Set expiry date" toggle changes.
 * @param onExpiryDateChanged Invoked with the instant the chosen day ends locally, in milliseconds.
 * @param onPasswordEnabled Invoked when the "Set password" toggle changes.
 * @param onPasswordChanged Invoked when the revealed password field text changes.
 * @param onSave Invoked when the bottom "Save" button is tapped.
 * @param onUpgrade Invoked when a free user chooses to see the Pro plans.
 * @param modifier Modifier for the scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSettingsScreen(
    uiState: LinkSettingsUiState,
    onBack: () -> Unit,
    onSeparateKeyEnabled: (Boolean) -> Unit,
    onLearnMore: () -> Unit,
    onExpiryEnabled: (Boolean) -> Unit,
    onExpiryDateChanged: (Long) -> Unit,
    onPasswordEnabled: (Boolean) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSave: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showUpgradeDialog by rememberSaveable { mutableStateOf(false) }
    val onCloseRequest = {
        if (uiState.hasUnsavedChanges) showDiscardDialog = true else onBack()
    }

    LaunchedOnceEffect(Unit) {
        Analytics.tracker.trackEvent(
            if (uiState.isAlbum) AlbumLinkSettingsScreenEvent else LinkSettingsScreenEvent
        )
    }
    LaunchedEffect(showDiscardDialog) {
        if (showDiscardDialog) Analytics.tracker.trackEvent(LinkDiscardChangesDialogEvent)
    }
    LaunchedEffect(uiState.errorEvent) {
        if (uiState.errorEvent == triggered) {
            Analytics.tracker.trackEvent(LinkSettingsSaveFailedEvent)
        }
    }

    val onSeparateKeyToggled = { enabled: Boolean ->
        trackSeparateKeyToggle(uiState, enabled)
        onSeparateKeyEnabled(enabled)
    }
    // Pro-only rows stay interactive for free accounts: turning one on opens the upgrade prompt
    // instead of applying the setting, matching legacy Get link.
    val requestUpgrade = {
        Analytics.tracker.trackEvent(
            if (uiState.isFolder) {
                LinkUpgradeToProFeatureFolderDialogEvent
            } else {
                LinkUpgradeToProFeatureFileDialogEvent
            }
        )
        showUpgradeDialog = true
    }
    val onExpiryToggled = { enabled: Boolean ->
        if (uiState.isProFeatureLocked) {
            requestUpgrade()
        } else {
            trackExpiryToggle(uiState.isFolder, enabled)
            onExpiryEnabled(enabled)
        }
    }
    val onPasswordToggled = { enabled: Boolean ->
        if (uiState.isProFeatureLocked) {
            requestUpgrade()
        } else {
            trackPasswordToggle(uiState, enabled)
            onPasswordEnabled(enabled)
        }
    }
    val onSaveClick = {
        Analytics.tracker.trackEvent(LinkSettingsSaveButtonPressedEvent)
        trackPasswordCommit(uiState)
        onSave()
    }
    val onLearnMoreClick = {
        Analytics.tracker.trackEvent(LinkSeparateKeyLearnMoreButtonPressedEvent)
        onLearnMore()
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
                // safeDrawing takes the larger of the navigation bar and the keyboard, so Save
                // rides above the keyboard without stacking both insets when it is open.
                AnchoredButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                        ),
                    buttonGroup = listOf {
                        Button.PrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(LINK_SETTINGS_SAVE_BUTTON_TAG),
                            text = stringResource(sharedR.string.general_action_save),
                            onClick = onSaveClick,
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
                    onSeparateKeyEnabled = onSeparateKeyToggled,
                    onLearnMore = onLearnMoreClick,
                    onExpiryEnabled = onExpiryToggled,
                    onExpiryDateChanged = onExpiryDateChanged,
                    onPasswordEnabled = onPasswordToggled,
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
                Analytics.tracker.trackEvent(LinkDiscardChangesDiscardButtonPressedEvent)
                showDiscardDialog = false
                onBack()
            },
            negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
            onNegativeButtonClicked = {
                Analytics.tracker.trackEvent(LinkDiscardChangesCancelButtonPressedEvent)
                showDiscardDialog = false
            },
            onDismiss = { showDiscardDialog = false },
        )
    }

    if (showUpgradeDialog) {
        BasicDialog(
            modifier = Modifier.testTag(LINK_SETTINGS_UPGRADE_DIALOG_TAG),
            title = stringResource(sharedR.string.share_link_upgrade_pro_dialog_title),
            description = stringResource(sharedR.string.share_link_upgrade_pro_dialog_message),
            positiveButtonText = stringResource(sharedR.string.share_link_upgrade_pro_dialog_see_plans),
            onPositiveButtonClicked = {
                Analytics.tracker.trackEvent(
                    if (uiState.isFolder) {
                        LinkProFeatureSeePlanFolderButtonPressedEvent
                    } else {
                        LinkProFeatureSeePlanFileButtonPressedEvent
                    }
                )
                showUpgradeDialog = false
                onUpgrade()
            },
            negativeButtonText = stringResource(sharedR.string.share_link_upgrade_pro_dialog_not_now),
            onNegativeButtonClicked = {
                Analytics.tracker.trackEvent(
                    if (uiState.isFolder) {
                        LinkProFeatureSeeNotNowPlanFolderButtonPressedEvent
                    } else {
                        LinkProFeatureSeeNotNowPlanFileButtonPressedEvent
                    }
                )
                showUpgradeDialog = false
            },
            onDismiss = { showUpgradeDialog = false },
        )
    }
}

@Composable
private fun LinkSettingsContent(
    uiState: LinkSettingsUiState,
    onSeparateKeyEnabled: (Boolean) -> Unit,
    onLearnMore: () -> Unit,
    onExpiryEnabled: (Boolean) -> Unit,
    onExpiryDateChanged: (Long) -> Unit,
    onPasswordEnabled: (Boolean) -> Unit,
    onPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val passwordFieldPosition = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Enabling expiry with no date yet goes straight to the picker, so choosing a date does not
    // need a second tap on the revealed field.
    // A Pro-locked tap only opens the upgrade prompt, so the picker must stay shut — otherwise both
    // would appear at once.
    val onExpiryToggled = { enabled: Boolean ->
        if (enabled && !uiState.isProFeatureLocked && uiState.expiryDate == null) {
            showDatePicker = true
        }
        onExpiryEnabled(enabled)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // A password-protected link already encrypts the key, so the option is unavailable while a
        // password is set. The reverse is allowed: enabling a password clears the separate key.
        GenericListItem(
            modifier = Modifier.testTag(LINK_SETTINGS_SEPARATE_KEY_ROW_TAG),
            title = {
                MegaText(
                    text = stringResource(sharedR.string.share_link_separate_key_title),
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                )
            },
            subtitle = {
                MegaText(
                    text = stringResource(sharedR.string.share_link_separate_key_subtitle),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                LinkSpannedText(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag(LINK_SETTINGS_SEPARATE_KEY_LEARN_MORE_TAG),
                    value = "[A]${stringResource(sharedR.string.general_learn_more)}[/A]",
                    spanStyles = mapOf(
                        SpanIndicator('A') to SpanStyleWithAnnotation(
                            megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                                spanStyle = SpanStyle(),
                                linkColor = LinkColor.Primary,
                            ),
                            annotation = LEARN_MORE_ANNOTATION,
                        )
                    ),
                    baseStyle = AppTheme.typography.bodyMedium,
                    onAnnotationClick = { onLearnMore() },
                )
            },
            enableClick = !uiState.isPasswordEnabled,
            onClickListener = { onSeparateKeyEnabled(!uiState.isSeparateKeyEnabled) },
            trailingElement = {
                Toggle(
                    modifier = Modifier.testTag(LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG),
                    isChecked = uiState.isSeparateKeyEnabled,
                    isEnabled = !uiState.isPasswordEnabled,
                    onCheckedChange = onSeparateKeyEnabled,
                )
            },
        )
        // Album links support neither expiry nor a password, so those rows are omitted rather than
        // disabled — there is nothing to unlock and no upgrade to offer.
        if (!uiState.isAlbum) {
            FlexibleLineListItem(
                modifier = Modifier.testTag(LINK_SETTINGS_EXPIRY_ROW_TAG),
                title = stringResource(sharedR.string.share_link_set_expiry_date),
                subtitle = stringResource(sharedR.string.share_link_expiry_subtitle),
                titleTrailingElement = if (uiState.isProFeatureLocked) {
                    { ProBadge(Modifier.testTag(LINK_SETTINGS_EXPIRY_PRO_BADGE_TAG)) }
                } else null,
                enableClick = true,
                onClickListener = { onExpiryToggled(!uiState.isExpiryEnabled) },
                trailingElement = {
                    Toggle(
                        modifier = Modifier.testTag(LINK_SETTINGS_EXPIRY_TOGGLE_TAG),
                        isChecked = uiState.isExpiryEnabled,
                        onCheckedChange = onExpiryToggled,
                    )
                },
            )
            AnimatedVisibility(visible = uiState.isExpiryEnabled) {
                ExpiryDateField(
                    expiryDate = uiState.expiryDate,
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            FlexibleLineListItem(
                modifier = Modifier.testTag(LINK_SETTINGS_PASSWORD_ROW_TAG),
                title = stringResource(sharedR.string.share_link_set_password),
                subtitle = stringResource(sharedR.string.share_link_password_subtitle),
                titleTrailingElement = if (uiState.isProFeatureLocked) {
                    { ProBadge(Modifier.testTag(LINK_SETTINGS_PASSWORD_PRO_BADGE_TAG)) }
                } else null,
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
                // The field sits at the bottom of the screen, so on focus the whole block —
                // strength help text included — is scrolled above the keyboard.
                Column(modifier = Modifier.bringIntoViewRequester(passwordFieldPosition)) {
                    PasswordTextInputField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag(LINK_SETTINGS_PASSWORD_FIELD_TAG),
                        label = null,
                        placeholder = stringResource(sharedR.string.password_placeholder),
                        text = uiState.password.orEmpty(),
                        showClearIcon = true,
                        onValueChanged = onPasswordChanged,
                        onFocusChanged = { focused ->
                            if (focused) {
                                coroutineScope.launch { passwordFieldPosition.bringIntoView() }
                            }
                        },
                    )
                    PasswordStrengthHelpText(
                        strength = uiState.passwordStrength,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .testTag(LINK_SETTINGS_PASSWORD_STRENGTH_TAG),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        MegaDatePickerDialog(
            confirmText = stringResource(sharedR.string.general_ok_only),
            dismissText = stringResource(sharedR.string.general_dialog_cancel_button),
            initialSelectedTimeMillis = uiState.expiryDate?.let(::utcMidnightOfLocalDay),
            selectableDates = SelectableExpiryDates,
            onDateSelected = {
                onExpiryDateChanged(endOfLocalDay(it))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** The separate-key toggle is the one link option reported per subject: album, folder or file. */
private fun trackSeparateKeyToggle(uiState: LinkSettingsUiState, enabled: Boolean) {
    Analytics.tracker.trackEvent(
        when {
            enabled && uiState.isAlbum -> LinkSendDecryptionKeyAlbumButtonEnabledEvent
            enabled && uiState.isFolder -> LinkSendDecryptionKeyFolderButtonEnabledEvent
            enabled -> LinkSendDecryptionKeyFileButtonEnabledEvent
            uiState.isAlbum -> LinkSendDecryptionKeyAlbumButtonDisabledEvent
            uiState.isFolder -> LinkSendDecryptionKeyFolderButtonDisabledEvent
            else -> LinkSendDecryptionKeyFileButtonDisabledEvent
        }
    )
}

private fun trackExpiryToggle(isFolder: Boolean, enabled: Boolean) {
    Analytics.tracker.trackEvent(
        when {
            enabled && isFolder -> LinkSetExpiryDateFolderButtonPressedEnabledEvent
            enabled -> LinkSetExpiryDateFileButtonPressedEnabledEvent
            isFolder -> LinkSetExpiryDateFolderButtonPressedDisabledEvent
            else -> LinkSetExpiryDateFileButtonPressedDisabledEvent
        }
    )
}

private fun trackPasswordToggle(uiState: LinkSettingsUiState, enabled: Boolean) {
    val event = when {
        enabled && uiState.isFolder -> LinkSetPasswordFolderButtonPressedEvent
        enabled -> LinkSetPasswordFileButtonPressedEvent
        !uiState.isPasswordAlreadySet -> return
        uiState.isFolder -> LinkRemovePasswordFolderButtonPressedEvent
        else -> LinkRemovePasswordFileButtonPressedEvent
    }
    Analytics.tracker.trackEvent(event)
}

/**
 * Save is the commit action for the password, so it stands in for the legacy screen's
 * "Set"/"Reset" button: a first-time password reports confirm, replacing an existing one
 * reports reset.
 */
private fun trackPasswordCommit(uiState: LinkSettingsUiState) {
    if (!uiState.isPasswordEnabled || uiState.password.isNullOrBlank()) return
    val event = when {
        !uiState.isPasswordAlreadySet && uiState.isFolder -> LinkConfirmPasswordFolderButtonPressedEvent
        !uiState.isPasswordAlreadySet -> LinkConfirmPasswordFileButtonPressedEvent
        uiState.password == uiState.initialPassword -> return
        uiState.isFolder -> LinkResetPasswordFolderButtonPressedEvent
        else -> LinkResetPasswordFileButtonPressedEvent
    }
    Analytics.tracker.trackEvent(event)
}

/**
 * A link expiry cannot be in the past, nor beyond what the API accepts — see [MAX_EXPIRY_SECONDS].
 * Bounding the picker keeps the out-of-range save, which fails with only a generic error, out of
 * reach rather than letting the user commit to a date first.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object SelectableExpiryDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        isSelectableExpiryDay(utcTimeMillis)

    override fun isSelectableYear(year: Int): Boolean =
        year in Calendar.getInstance().get(Calendar.YEAR)..maxExpiryYear()
}

@Composable
private fun ProBadge(modifier: Modifier = Modifier) {
    Badge(
        modifier = modifier,
        badgeType = BadgeType.MegaSecondary,
        text = stringResource(sharedR.string.general_pro_label),
    )
}

@Composable
private fun ExpiryDateField(
    expiryDate: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateText = remember(expiryDate) { expiryDate?.let(::formatExpiryDate).orEmpty() }
    val openPickerLabel = stringResource(sharedR.string.share_link_set_expiry_date)
    Box(modifier = modifier) {
        ReadOnlyTextInputField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LINK_SETTINGS_EXPIRY_FIELD_TAG),
            text = dateText,
            trailingIcon = {
                MegaIcon(
                    painter = painterResource(iconPackR.drawable.ic_calendar_01_medium_thin_outline),
                    tint = IconColor.Secondary,
                    contentDescription = null,
                )
            },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = openPickerLabel,
                    onClick = onClick,
                ),
        )
    }
}

/**
 * The password strength label, coloured by strength.
 *
 * Only the help text carries the strength colour — it is deliberately not fed to the input field's
 * `successText`/`warningText`/`errorText`, which would also recolour the field's border. The field
 * keeps its default accent-when-focused styling.
 */
@Composable
private fun PasswordStrengthHelpText(
    strength: PasswordStrength?,
    modifier: Modifier = Modifier,
) {
    if (strength.isTooWeakForLink) {
        HelpTextError(
            modifier = modifier,
            text = stringResource(sharedR.string.password_too_weak_error_message),
        )
    } else {
        val level = strength?.toLinkPasswordStrength() ?: return
        val label = stringResource(level.labelRes)
        when (level) {
            LinkPasswordStrength.Weak -> HelpTextError(modifier = modifier, text = label)
            LinkPasswordStrength.Moderate -> HelpTextWarning(modifier = modifier, text = label)
            LinkPasswordStrength.Strong -> HelpTextSuccess(modifier = modifier, text = label)
        }
    }
}

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
            onSeparateKeyEnabled = {},
            onLearnMore = {},
            onExpiryEnabled = {},
            onExpiryDateChanged = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
            onUpgrade = {},
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
            onSeparateKeyEnabled = {},
            onLearnMore = {},
            onExpiryEnabled = {},
            onExpiryDateChanged = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
            onUpgrade = {},
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
            onSeparateKeyEnabled = {},
            onLearnMore = {},
            onExpiryEnabled = {},
            onExpiryDateChanged = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
            onUpgrade = {},
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
private fun LinkSettingsScreenAlbumPreview() {
    AndroidThemeForPreviews {
        LinkSettingsScreen(
            uiState = previewData.copy(isAlbum = true),
            onBack = {},
            onSeparateKeyEnabled = {},
            onLearnMore = {},
            onExpiryEnabled = {},
            onExpiryDateChanged = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
            onUpgrade = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LinkSettingsScreenLoadingPreview() {
    AndroidThemeForPreviews {
        LinkSettingsScreen(
            uiState = LinkSettingsUiState(isLoading = true),
            onBack = {},
            onSeparateKeyEnabled = {},
            onLearnMore = {},
            onExpiryEnabled = {},
            onExpiryDateChanged = {},
            onPasswordEnabled = {},
            onPasswordChanged = {},
            onSave = {},
            onUpgrade = {},
        )
    }
}

internal const val LINK_SETTINGS_APP_BAR_TAG = "link_settings_screen:app_bar"
internal const val LINK_SETTINGS_SAVE_BUTTON_TAG = "link_settings_screen:button_save"
internal const val LINK_SETTINGS_SEPARATE_KEY_ROW_TAG = "link_settings_screen:row_separate_key"
internal const val LINK_SETTINGS_SEPARATE_KEY_TOGGLE_TAG =
    "link_settings_screen:toggle_separate_key"
internal const val LINK_SETTINGS_UPGRADE_DIALOG_TAG = "link_settings_screen:upgrade_dialog"
internal const val LINK_SETTINGS_PASSWORD_STRENGTH_TAG = "link_settings_screen:password_strength"
internal const val LINK_SETTINGS_SEPARATE_KEY_LEARN_MORE_TAG =
    "link_settings_screen:separate_key_learn_more"
private const val LEARN_MORE_ANNOTATION = "learn_more"
internal const val LINK_SETTINGS_EXPIRY_ROW_TAG = "link_settings_screen:row_expiry"
internal const val LINK_SETTINGS_EXPIRY_TOGGLE_TAG = "link_settings_screen:toggle_expiry"
internal const val LINK_SETTINGS_EXPIRY_PRO_BADGE_TAG = "link_settings_screen:pro_badge_expiry"
internal const val LINK_SETTINGS_PASSWORD_PRO_BADGE_TAG = "link_settings_screen:pro_badge_password"
internal const val LINK_SETTINGS_EXPIRY_FIELD_TAG = "link_settings_screen:field_expiry"
internal const val LINK_SETTINGS_PASSWORD_ROW_TAG = "link_settings_screen:row_password"
internal const val LINK_SETTINGS_PASSWORD_TOGGLE_TAG = "link_settings_screen:toggle_password"
internal const val LINK_SETTINGS_PASSWORD_FIELD_TAG = "link_settings_screen:field_password"
internal const val LINK_SETTINGS_LOADING_TAG = "link_settings_screen:loading"
internal const val LINK_SETTINGS_DISCARD_DIALOG_TAG = "link_settings_screen:discard_dialog"
