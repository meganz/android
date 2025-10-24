package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.resources.R
import timber.log.Timber

@Composable
fun SubscriptionInformation(context: Context) {
    MegaText(
        modifier = Modifier
            .padding(top = LocalSpacing.current.x16)
            .padding(horizontal = 16.dp)
            .testTag(TEST_TAG_SUBSCRIPTION_INFO_TITLE),
        text = stringResource(id = R.string.choose_account_screen_subscription_information_title),
        textColor = TextColor.Primary,
        style = AppTheme.typography.titleSmall
    )
    LinkSpannedText(
        modifier = Modifier
            .padding(
                top = LocalSpacing.current.x8,
                start = LocalSpacing.current.x16,
                end = LocalSpacing.current.x16
            )
            .testTag(TEST_TAG_SUBSCRIPTION_INFO_DESC),
        value = stringResource(id = R.string.choose_account_screen_subscription_information_description),
        spanStyles = mapOf(
            SpanIndicator('A') to SpanStyleWithAnnotation(
                megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                    spanStyle = SpanStyle(),
                    linkColor = LinkColor.Primary
                ),
                annotation = stringResource(id = R.string.choose_account_screen_subscription_information_description)
                    .substringAfter("[A]")
                    .substringBefore("[/A]")
            )
        ),
        baseTextColor = TextColor.Secondary,
        baseStyle = AppTheme.typography.bodySmall,
        onAnnotationClick = {
            context.navigateToPlayStoreAccountSubscription()
        }
    )
    val termsText =
        stringResource(id = R.string.choose_account_screen_terms_and_policies_link_text)
            .substringAfter("[A]")
            .substringBefore("[/A]")

    val privacyText =
        stringResource(id = R.string.choose_account_screen_terms_and_policies_link_text)
            .substringAfter("[B]")
            .substringBefore("[/B]")

    LinkSpannedText(
        modifier = Modifier
            .padding(
                top = LocalSpacing.current.x24,
                start = LocalSpacing.current.x16,
                end = LocalSpacing.current.x16,
                bottom = LocalSpacing.current.x48
            )
            .testTag(TEST_TAG_TERMS_AND_POLICIES),
        value = stringResource(id = R.string.choose_account_screen_terms_and_policies_link_text),
        spanStyles = mapOf(
            SpanIndicator('A') to SpanStyleWithAnnotation(
                megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                    spanStyle = SpanStyle(),
                    linkColor = LinkColor.Primary
                ),
                annotation = termsText
            ),
            SpanIndicator('B') to SpanStyleWithAnnotation(
                megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                    spanStyle = SpanStyle(),
                    linkColor = LinkColor.Primary
                ),
                annotation = privacyText
            )
        ),
        baseStyle = AppTheme.typography.labelLarge,
        onAnnotationClick = { annotation ->
            val megaNavigator = context.megaNavigator
            when (annotation) {
                termsText -> megaNavigator.launchUrl(context, TERMS_OF_SERVICE_URL)
                privacyText -> megaNavigator.launchUrl(context, PRIVACY_POLICY_URL)
            }
        }
    )
}


private const val TERMS_OF_SERVICE_URL = "https://mega.io/terms"
private const val PRIVACY_POLICY_URL = "https://mega.io/privacy"

const val PLAY_STORE_ACCOUNT_SUBSCRIPTION_URL =
    "https://play.google.com/store/account/subscriptions"

private fun Context.navigateToPlayStoreAccountSubscription() {
    try {
        startActivity(Intent(ACTION_VIEW, PLAY_STORE_ACCOUNT_SUBSCRIPTION_URL.toUri()))
    } catch (e: ActivityNotFoundException) {
        Timber.e(e, "Play Store Subscription Page Not Found!")
    }
}