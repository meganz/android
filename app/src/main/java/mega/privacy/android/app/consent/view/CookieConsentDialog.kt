package mega.privacy.android.app.consent.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import mega.android.core.ui.components.dialogs.BasicImageDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.LinkColor
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

@Composable
fun CookieConsentDialog(
    closeDialog: () -> Unit,
    onAcceptCookies: () -> Unit,
    onAcceptEssentialCookies: () -> Unit,
    onOpenCookieSettings: () -> Unit,
    onNavigateToCookiePolicy: () -> Unit,
) {
    BasicImageDialog(
        modifier = Modifier,
        title = stringResource(R.string.dialog_cookie_alert_title),
        description = SpannableText(
            text = stringResource(R.string.dialog_cookie_alert_message),
            annotations = hashMapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    MegaSpanStyle.LinkColorStyle(
                        SpanStyle(),
                        LinkColor.Primary
                    ), "A"
                )
            ),
            onAnnotationClick = { onNavigateToCookiePolicy() }
        ),
        imagePainter = painterResource(R.drawable.il_cookie),
        positiveButtonText = stringResource(R.string.preference_cookies_accept),
        onPositiveButtonClicked = {
            closeDialog()
            onAcceptCookies()
        },
        negativeButtonText = stringResource(R.string.settings_about_cookie_settings),
        onNegativeButtonClicked = {
            onAcceptEssentialCookies()
            closeDialog()
            onOpenCookieSettings()
        },
    )
}

@CombinedThemePreviews
@Composable
private fun CookieConsentDialogPreview() {
    AndroidThemeForPreviews {
        CookieConsentDialog(
            closeDialog = { },
            onAcceptCookies = { },
            onAcceptEssentialCookies = { },
            onOpenCookieSettings = { },
            onNavigateToCookiePolicy = {},
        )
    }
}