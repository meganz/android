package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider.Companion.localisedSubscriptionsList
import mega.privacy.android.app.upgradeAccount.view.GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG
import mega.privacy.android.app.upgradeAccount.view.SUBSCRIPTION_DETAILS_DESCRIPTION_TAG
import mega.privacy.android.app.upgradeAccount.view.SUBSCRIPTION_DETAILS_TITLE_TAG
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.PLAY_STORE_SUBSCRIPTION_URL
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body2medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import java.util.Locale

/**
 * Composable UI for Subscription details to reuse on different screens
 *
 * @param onLinkClick   Function to handle the click on the link
 * @param chosenPlan   the AccountType of selected plan
 * @param subscriptionList   List of LocalisedSubscription
 * @param isMonthly   Boolean to know if the subscription is monthly or yearly
 */
@Composable
internal fun SubscriptionDetails(
    onLinkClick: (link: String) -> Unit,
    chosenPlan: AccountType,
    subscriptionList: List<LocalisedSubscription>,
    isMonthly: Boolean,
) {
    var subscriptionDetailsBodyString =
        stringResource(
            id = if (isMonthly) {
                R.string.account_upgrade_account_subscription_details_body_monthly_without_price
            } else {
                R.string.account_upgrade_account_subscription_details_body_yearly_without_price
            }
        )
    val subscription = subscriptionList.firstOrNull { it.accountType == chosenPlan }
    val formattedPrice = subscription?.localisePriceCurrencyCode(Locale.getDefault(), isMonthly)
    var testTag = if (isMonthly) "_monthly_no_price" else "_yearly_no_price"
    when (chosenPlan) {
        AccountType.PRO_LITE -> {
            if (formattedPrice != null) {
                subscriptionDetailsBodyString = stringResource(
                    id = if (isMonthly) {
                        R.string.account_upgrade_account_subscription_details_body_monthly_with_price
                    } else {
                        R.string.account_upgrade_account_subscription_details_body_yearly_with_price
                    },
                    formattedPrice.price,
                    formattedPrice.price
                )
                testTag = if (isMonthly) "_monthly_lite_with_price" else "_yearly_lite_with_price"
            }
        }

        AccountType.PRO_I -> {
            if (formattedPrice != null) {
                subscriptionDetailsBodyString = stringResource(
                    id = if (isMonthly) {
                        R.string.account_upgrade_account_subscription_details_body_monthly_with_price
                    } else {
                        R.string.account_upgrade_account_subscription_details_body_yearly_with_price
                    },
                    formattedPrice.price,
                    formattedPrice.price
                )
                testTag = if (isMonthly) "_monthly_pro_i_with_price" else "_yearly_pro_i_with_price"
            }
        }

        AccountType.PRO_II -> {
            if (formattedPrice != null) {
                subscriptionDetailsBodyString = stringResource(
                    id = if (isMonthly) {
                        R.string.account_upgrade_account_subscription_details_body_monthly_with_price
                    } else {
                        R.string.account_upgrade_account_subscription_details_body_yearly_with_price
                    },
                    formattedPrice.price,
                    formattedPrice.price
                )
                testTag =
                    if (isMonthly) "_monthly_pro_ii_with_price" else "_yearly_pro_ii_with_price"
            }
        }

        AccountType.PRO_III -> {
            if (formattedPrice != null) {
                subscriptionDetailsBodyString = stringResource(
                    id = if (isMonthly) {
                        R.string.account_upgrade_account_subscription_details_body_monthly_with_price
                    } else {
                        R.string.account_upgrade_account_subscription_details_body_yearly_with_price
                    },
                    formattedPrice.price,
                    formattedPrice.price
                )
                testTag =
                    if (isMonthly) "_monthly_pro_iii_with_price" else "_yearly_pro_iii_with_price"
            }
        }

        else -> {
            subscriptionDetailsBodyString = stringResource(
                id = if (isMonthly) {
                    R.string.account_upgrade_account_subscription_details_body_monthly_without_price
                } else {
                    R.string.account_upgrade_account_subscription_details_body_yearly_without_price
                }
            )
            testTag = if (isMonthly) "_monthly_no_price" else "_yearly_no_price"
        }
    }
    Column(
        modifier = Modifier
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 50.dp
            )
            .testTag(GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG)
    ) {
        MegaText(
            modifier = Modifier.testTag(SUBSCRIPTION_DETAILS_TITLE_TAG),
            text = stringResource(id = R.string.account_upgrade_account_description_subscription_title),
            style = MaterialTheme.typography.body2medium,
            textColor = TextColor.Primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MegaSpannedClickableText(
            modifier = Modifier.testTag("$SUBSCRIPTION_DETAILS_DESCRIPTION_TAG$testTag"),
            value = subscriptionDetailsBodyString,
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.None),
                        color = TextColor.Accent,
                    ), PLAY_STORE_SUBSCRIPTION_URL
                ),
                SpanIndicator('B') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.None),
                        color = TextColor.Accent,
                    ), Constants.TERMS_OF_SERVICE_URL
                ),
                SpanIndicator('C') to MegaSpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle(
                        spanStyle = SpanStyle(textDecoration = TextDecoration.None),
                        color = TextColor.Accent,
                    ),
                    annotation = "https://mega.nz/privacy"
                ),
            ),
            onAnnotationClick = onLinkClick,
            baseStyle = MaterialTheme.typography.body4.copy(lineHeight = 16.sp),
            color = TextColor.Primary
        )
        Spacer(modifier = Modifier.height(42.dp))
    }
}

@CombinedThemePreviews
@Composable
fun SubscriptionDetailsPreview() {
    OriginalTempTheme(isDark = false) {
        SubscriptionDetails(
            onLinkClick = {},
            chosenPlan = AccountType.PRO_LITE,
            subscriptionList = localisedSubscriptionsList,
            isMonthly = true
        )
    }
}
