package mega.privacy.android.app.presentation.cancelaccountplan.view

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.banners.PromptMessageBanner
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaRadioButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionWithCharacterLimitTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeTabletLandscapePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeTabletPortraitPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.isScreenOrientationLandscape
import mega.privacy.android.shared.original.core.ui.utils.isTablet
import mega.privacy.android.shared.resources.R

@Composable
internal fun CancelSubscriptionSurveyView(
    possibleCancellationReasons: List<Int>,
    onCancelSubscriptionButtonClicked: () -> Unit,
    onDoNotCancelButtonClicked: () -> Unit,
) {

    var showError by rememberSaveable { mutableStateOf(false) }
    var allowContact by rememberSaveable { mutableStateOf(false) }
    var selectedOptionPosition by rememberSaveable { mutableIntStateOf(-1) }
    var othersDescriptionText by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val horizontalPadding = if (isTablet()) {
        if (isScreenOrientationLandscape()) {
            390.dp
        } else {
            190.dp
        }
    } else {
        20.dp
    }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(
                horizontal = horizontalPadding, vertical = 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MegaText(
            text = stringResource(id = SharedR.string.account_cancel_subscription_survey_title),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.h6Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 30.dp)
                .testTag(TITLE_TEST_TAG),
        )
        MegaText(
            text = stringResource(id = SharedR.string.account_cancel_subscription_survey_cancellation_message),
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.subtitle1medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 20.dp)
                .testTag(SUBTITLE_TEST_TAG),
        )

        if (showError) {
            PromptMessageBanner(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .testTag(PROMPT_MESSAGE_BANNER_TEST_TAG),
                message = stringResource(
                    id = SharedR.string.account_cancel_subscription_survey_no_reason_selected
                )
            )
        }

        SurveyOptions(
            possibleCancellationReasons = possibleCancellationReasons,
            selectedOptionPosition = selectedOptionPosition,
            onItemClicked = { index ->
                selectedOptionPosition = index
                showError = false
            },
            modifier = Modifier
                .padding(top = 20.dp)
                .testTag(SURVEY_OPTIONS_GROUP_TEST_TAG)
        )

        if (selectedOptionPosition == possibleCancellationReasons.lastIndex) {
            GenericDescriptionWithCharacterLimitTextField(
                maxCharacterLimit = CHARACTER_LIMIT,
                value = othersDescriptionText,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .testTag(CANCEL_SUBSCRIPTION_SURVEY_OTHER_TEXT_FIELD_TEST_TAG),
                initiallyFocused = true,
                onValueChange = {
                    othersDescriptionText = it
                }, onClearText = {
                    othersDescriptionText = ""
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MegaCheckbox(
                checked = allowContact,
                onCheckedChange = { allowContact = it },
                rounded = false,
                modifier = Modifier
                    .size(20.dp)
                    .testTag(CANCEL_SUBSCRIPTION_SURVEY_ALLOW_CONTACT_CHECKBOX_TEST_TAG),
            )
            MegaText(
                text = stringResource(id = SharedR.string.account_cancel_subscription_survey_allow_contact),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle1medium,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag(CANCEL_SUBSCRIPTION_SURVEY_ALLOW_CONTACT_TEXT_TEST_TAG),
            )
        }
        Column {
            RaisedDefaultMegaButton(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .testTag(CANCEL_SUBSCRIPTION_BUTTON_TEST_TAG),
                text = stringResource(
                    id = SharedR.string.account_cancel_subscription_survey_cancel_button
                ),
                onClick = {
                    if (selectedOptionPosition == -1) {
                        showError = true
                    } else {
                        showError = false
                        if (othersDescriptionText.length <= CHARACTER_LIMIT)
                            onCancelSubscriptionButtonClicked()
                    }
                },
            )
            TextMegaButton(
                textId = SharedR.string.account_cancel_subscription_survey_do_not_cancel_button,
                onClick = onDoNotCancelButtonClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag(DO_NOT_CANCELLATION_BUTTON_TEST_TAG),
            )
        }
    }
}

@Composable
internal fun SurveyOptions(
    possibleCancellationReasons: List<Int>,
    onItemClicked: (Int) -> Unit,
    selectedOptionPosition: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        possibleCancellationReasons.forEachIndexed { index, reasonResId ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .selectable(
                        selected = (index == selectedOptionPosition),
                        onClick = {
                            onItemClicked(index)
                        },
                        role = Role.RadioButton
                    )
                    .testTag(SURVEY_OPTIONS_ROW_TEST_TAG)
            ) {
                MegaRadioButton(
                    selected = (index == selectedOptionPosition),
                    onClick = {
                        onItemClicked(index)
                    },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically)
                        .testTag(SURVEY_OPTIONS_OPTION_RADIO_TEST_TAG),
                )
                MegaText(
                    text = stringResource(id = reasonResId),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp)
                        .testTag(SURVEY_OPTIONS_OPTION_TEXT_TEST_TAG),
                )
            }
        }
    }
}

@Composable
@CombinedThemePreviews
@CombinedThemeTabletLandscapePreviews
@CombinedThemeTabletPortraitPreviews
private fun CancelSubscriptionSurveyViewPreview() {
    val possibleCancellationReasons = listOf(
        R.string.account_cancel_subscription_survey_option_expensive,
        R.string.account_cancel_subscription_survey_option_cannot_afford,
        R.string.account_cancel_subscription_survey_option_no_subscription,
        R.string.account_cancel_subscription_survey_option_no_storage_need,
        R.string.account_cancel_subscription_survey_option_missing_features,
        R.string.account_cancel_subscription_survey_option_switch_provider,
        R.string.account_cancel_subscription_survey_option_confusing,
        R.string.account_cancel_subscription_survey_option_dissatisfied_support,
        R.string.account_cancel_subscription_survey_option_temporary_use,
    ).shuffled() + R.string.account_cancel_subscription_survey_option_other
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CancelSubscriptionSurveyView(
            possibleCancellationReasons = possibleCancellationReasons,
            onCancelSubscriptionButtonClicked = {},
            onDoNotCancelButtonClicked = {},
        )
    }
}

internal const val CHARACTER_LIMIT = 120

internal const val SURVEY_OPTIONS_GROUP_TEST_TAG =
    "cancel_subscription_survey_view:options_group"
internal const val SURVEY_OPTIONS_OPTION_RADIO_TEST_TAG =
    "survey_options:mega_radio_button"
internal const val SURVEY_OPTIONS_OPTION_TEXT_TEST_TAG =
    "survey_options:option_text"
internal const val CANCEL_SUBSCRIPTION_SURVEY_OTHER_TEXT_FIELD_TEST_TAG =
    "cancel_subscription_survey_view:text_field_with_character_limit"
internal const val CANCEL_SUBSCRIPTION_SURVEY_ALLOW_CONTACT_CHECKBOX_TEST_TAG =
    "cancel_subscription_survey_view:allow_contact_checkbox"
internal const val CANCEL_SUBSCRIPTION_SURVEY_ALLOW_CONTACT_TEXT_TEST_TAG =
    "cancel_subscription_survey_view:allow_contact_text"
internal const val PROMPT_MESSAGE_BANNER_TEST_TAG =
    "cancel_subscription_survey_view:prompt_message_banner"
internal const val CANCEL_SUBSCRIPTION_BUTTON_TEST_TAG =
    "cancel_subscription_survey_view:cancel_subscription_button"
internal const val DO_NOT_CANCELLATION_BUTTON_TEST_TAG =
    "cancel_subscription_survey_view:do_not_cancel_button"
internal const val SUBTITLE_TEST_TAG =
    "cancel_subscription_survey_view:text_subtitle"
internal const val TITLE_TEST_TAG =
    "cancel_subscription_survey_view:text_title"
internal const val SURVEY_OPTIONS_ROW_TEST_TAG =
    "survey_options:row"
