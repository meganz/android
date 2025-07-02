package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.inputfields.HelpTextError
import mega.android.core.ui.components.inputfields.HelpTextInfo
import mega.android.core.ui.components.inputfields.HelpTextSuccess
import mega.android.core.ui.components.inputfields.HelpTextWarning
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun PasswordHint(
    modifier: Modifier,
    isVisible: Boolean,
    doesContainMixedCase: Boolean,
    doesContainNumberOrSpecialCharacter: Boolean,
    isMinimumCharacterError: Boolean,
    passwordStrength: PasswordStrength,
    isTitleVisible: Boolean = true,
) {
    AnimatedVisibility(visible = isVisible) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            if (isTitleVisible) {
                HintTitle(
                    modifier = Modifier.padding(bottom = LocalSpacing.current.x16),
                    isMinimumCharacterError = isMinimumCharacterError,
                    passwordStrength = passwordStrength
                )
            }

            MegaText(
                modifier = Modifier.testTag(PASSWORD_HINT_RULES_HEADLINE_TAG),
                text = stringResource(id = sharedR.string.sign_up_password_hint_title_text),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.titleSmall,
            )

            HintChecklistItem(
                modifier = Modifier
                    .padding(top = LocalSpacing.current.x12)
                    .testTag(PASSWORD_HINT_UPPER_LOWER_CASE_RULE_TAG),
                text = stringResource(id = sharedR.string.sign_up_password_hint_upper_lower_case),
                isChecklistDone = doesContainMixedCase
            )

            HintChecklistItem(
                modifier = Modifier
                    .padding(top = LocalSpacing.current.x8)
                    .testTag(PASSWORD_HINT_ONE_NUMBER_SPECIAL_CHARACTER_RULE_TAG),
                text = stringResource(id = sharedR.string.sign_up_password_hint_number_or_special_character),
                isChecklistDone = doesContainNumberOrSpecialCharacter
            )
        }
    }
}

@Composable
private fun HintTitle(
    modifier: Modifier,
    isMinimumCharacterError: Boolean,
    passwordStrength: PasswordStrength,
) {
    when {
        isMinimumCharacterError -> {
            HelpTextError(
                modifier = modifier,
                text = stringResource(id = sharedR.string.sign_up_password_min_character_error_message),
            )
        }

        passwordStrength == PasswordStrength.VERY_WEAK || passwordStrength == PasswordStrength.WEAK -> {
            HelpTextError(
                modifier = modifier,
                text = stringResource(id = sharedR.string.sign_up_password_weak_password_error_message),
            )
        }

        passwordStrength == PasswordStrength.MEDIUM -> {
            HelpTextWarning(
                modifier = modifier,
                text = stringResource(id = sharedR.string.sign_up_password_medium_password_warning_message),
            )
        }

        passwordStrength == PasswordStrength.GOOD || passwordStrength == PasswordStrength.STRONG -> {
            HelpTextSuccess(
                modifier = modifier,
                text = stringResource(id = sharedR.string.sign_up_password_min_character_error_message),
                textColor = TextColor.Primary,
            )
        }

        else -> {
            HelpTextInfo(
                modifier = modifier,
                text = stringResource(id = sharedR.string.sign_up_password_min_character_error_message),
            )
        }
    }
}

@Composable
private fun HintChecklistItem(
    text: String,
    modifier: Modifier = Modifier,
    isChecklistDone: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (isChecklistDone) {
            MegaIcon(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterVertically)
                    .testTag(PASSWORD_HINT_CHECK_ICON_TAG + text),
                painter = rememberVectorPainter(IconPack.Medium.Regular.Outline.Check),
                supportTint = SupportColor.Success,
                contentDescription = text
            )
        } else {
            MegaIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .testTag(PASSWORD_HINT_CIRCLE_ICON_TAG + text),
                painter = rememberVectorPainter(IconPack.Medium.Regular.Outline.CircleSmall),
                tint = IconColor.Secondary,
                contentDescription = text
            )
        }

        MegaText(
            modifier = Modifier
                .padding(start = LocalSpacing.current.x8)
                .align(Alignment.CenterVertically),
            text = text,
            style = AppTheme.typography.bodySmall,
            textColor = TextColor.Secondary,
        )
    }
}

@Composable
@CombinedThemePreviews
private fun PasswordHintPreview() {
    AndroidThemeForPreviews {
        PasswordHint(
            modifier = Modifier,
            isVisible = true,
            doesContainMixedCase = true,
            doesContainNumberOrSpecialCharacter = true,
            isMinimumCharacterError = false,
            passwordStrength = PasswordStrength.GOOD,
        )
    }
}

internal const val PASSWORD_HINT_RULES_HEADLINE_TAG = "password_hint:text_password_rules_headline"
internal const val PASSWORD_HINT_UPPER_LOWER_CASE_RULE_TAG =
    "password_hint:text_upper_and_lower_case"
internal const val PASSWORD_HINT_ONE_NUMBER_SPECIAL_CHARACTER_RULE_TAG =
    "password_hint:text_one_number_or_special_character"
internal const val PASSWORD_HINT_CHECK_ICON_TAG = "hint_checklist_item:icon_check"
internal const val PASSWORD_HINT_CIRCLE_ICON_TAG = "hint_checklist_item:icon_circle"
