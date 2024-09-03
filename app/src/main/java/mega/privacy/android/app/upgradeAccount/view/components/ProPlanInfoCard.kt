package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.extensions.toUIAccountType
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.body2
import mega.privacy.android.shared.original.core.ui.theme.caption
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_050_grey_800
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale


/**
 * common component for screen with Pro plans info, e.g. UpgradeAccountView or Onboarding dialog Variant B
 * to show info about any Pro plan which user can buy in the app (e.g. Pro I/II/III)
 */
@Composable
internal fun ProPlanInfoCard(
    proPlan: AccountType,
    subscription: LocalisedSubscription,
    baseStorageFormatted: String,
    isRecommended: Boolean,
    onPlanClicked: () -> Unit,
    isMonthly: Boolean,
    isClicked: Boolean,
    showCurrentPlanLabel: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val isFreePlan = proPlan == AccountType.FREE
    val storageValueString =
        if (isFreePlan) {
            stringResource(id = sharedR.string.general_size_giga_byte, baseStorageFormatted)
        } else {
            stringResource(
                id = subscription.formatStorageSize().unit,
                subscription.formatStorageSize().size
            )
        }
    val transferValueString =

        if (isFreePlan) {
            stringResource(id = sharedR.string.dialog_onboarding_transfer_quota_free_limited)
        } else {
            stringResource(
                id = subscription.formatTransferSize(isMonthly).unit,
                subscription.formatTransferSize(isMonthly).size
            )
        }

    val uiAccountType = proPlan.toUIAccountType()

    val storageString = stringResource(
        id = R.string.account_upgrade_account_pro_plan_info_storage,
        storageValueString
    )
    val transferString = stringResource(
        id = R.string.account_upgrade_account_pro_plan_info_transfer,
        transferValueString
    )

    val formattedPrice = subscription.localisePriceCurrencyCode(Locale.getDefault(), isMonthly)
    val priceString =
        if (isMonthly) stringResource(
            id = R.string.account_upgrade_account_pro_plan_info_monthly_price,
            formattedPrice.price,
            formattedPrice.currencyCode
        )
        else stringResource(
            id = R.string.account_upgrade_account_pro_plan_info_yearly_price,
            formattedPrice.price,
            formattedPrice.currencyCode
        )

    Card(shape = RoundedCornerShape(12.dp),
        elevation = if (isClicked) 8.dp else 0.dp,
        modifier = modifier
            .padding(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp
            )
            .border(
                width = 1.dp,
                color =
                if (isClicked) MaterialTheme.colors.teal_300_teal_200
                else MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onPlanClicked() }
            .testTag("$testTag${uiAccountType.ordinal}")) {
        Column {
            Row {
                MegaText(
                    text = stringResource(id = uiAccountType.textValue),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1medium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 12.dp,
                        end = 8.dp,
                        bottom = 12.dp
                    ),
                )
                if (showCurrentPlanLabel) {
                    MegaText(
                        text = stringResource(id = R.string.account_upgrade_account_pro_plan_info_current_plan_label),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight(600)),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.grey_050_grey_800,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterVertically)
                            .padding(
                                horizontal = 8.dp, vertical = 4.dp
                            )
                            .testTag("$testTag$CURRENT_PLAN_TAG")
                    )
                }
                if (isRecommended) {
                    MegaText(
                        text = stringResource(id = R.string.account_upgrade_account_pro_plan_info_recommended_label),
                        textColor = TextColor.OnColor,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight(600)),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.teal_300_teal_200,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterVertically)
                            .padding(
                                horizontal = 8.dp, vertical = 4.dp
                            )
                            .testTag("$testTag$RECOMMENDED_PLAN_TAG")
                    )
                }
            }
            Divider(
                thickness = 0.4.dp,
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                modifier = Modifier.padding(horizontal = 1.dp)
            )
            Row(
                modifier = Modifier.padding(
                    vertical = 16.dp,
                    horizontal = 16.dp
                )
            ) {
                Column(modifier = Modifier.weight(0.5f)) {
                    MegaSpannedText(
                        value = storageString,
                        baseStyle = body2,
                        styles = hashMapOf(
                            SpanIndicator('A') to MegaSpanStyle(
                                spanStyle = SpanStyle(fontWeight = FontWeight.Medium),
                                color = TextColor.Primary,
                            ),
                            SpanIndicator('B') to MegaSpanStyle(
                                color = TextColor.Secondary,
                            )
                        ),
                        color = TextColor.Primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    MegaSpannedText(
                        value = transferString,
                        baseStyle = body2,
                        styles = hashMapOf(
                            SpanIndicator('A') to MegaSpanStyle(
                                spanStyle = SpanStyle(fontWeight = FontWeight.Medium),
                                color = TextColor.Primary,
                            ),
                            SpanIndicator('B') to MegaSpanStyle(
                                color = TextColor.Secondary,
                            )
                        ),
                        color = TextColor.Primary,
                    )
                }
                Column(
                    modifier = Modifier.weight(0.5f),
                    horizontalAlignment = Alignment.End
                ) {
                    if (isFreePlan)
                        MegaText(
                            text = stringResource(id = sharedR.string.dialog_onboarding_free_account_price),
                            textColor = TextColor.Primary,
                            style = MaterialTheme.typography.h6Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    else
                        MegaSpannedText(
                            value = priceString,
                            baseStyle = caption,
                            styles = hashMapOf(
                                SpanIndicator('A') to MegaSpanStyle(
                                    spanStyle = SpanStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextColor.Primary,
                                ),
                                SpanIndicator('B') to MegaSpanStyle(
                                    color = TextColor.Secondary,
                                )
                            ),
                            color = TextColor.Primary,
                            modifier = Modifier
                                .padding(
                                    start = 24.dp,
                                    top = 3.dp,
                                ),
                            textAlign = TextAlign.End
                        )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
fun ProPlanInfoCardPreview() {
    val localisedPriceStringMapper = LocalisedPriceStringMapper()
    val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    val formattedSizeMapper = FormattedSizeMapper()
    val subscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            99.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ProPlanInfoCard(
            proPlan = AccountType.PRO_I,
            subscription = subscriptionProI,
            baseStorageFormatted = "20 GB",
            isRecommended = true,
            onPlanClicked = { /*TODO*/ },
            isMonthly = true,
            isClicked = false,
            showCurrentPlanLabel = false,
            testTag = "upgrade_account_screen:card_pro_plan_",
        )
    }
}

@CombinedThemePreviews
@Composable
fun FreePlanInfoCardPreview() {
    val localisedPriceStringMapper = LocalisedPriceStringMapper()
    val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    val formattedSizeMapper = FormattedSizeMapper()
    val subscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            99.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ProPlanInfoCard(
            proPlan = AccountType.FREE,
            subscription = subscriptionProI,
            baseStorageFormatted = "20 GB",
            isRecommended = false,
            onPlanClicked = { /*TODO*/ },
            isMonthly = true,
            isClicked = false,
            showCurrentPlanLabel = false,
            testTag = "upgrade_account_screen:card_pro_plan_",
        )
    }
}

internal const val CURRENT_PLAN_TAG = "label_current_plan"
internal const val RECOMMENDED_PLAN_TAG = "label_recommended_plan"