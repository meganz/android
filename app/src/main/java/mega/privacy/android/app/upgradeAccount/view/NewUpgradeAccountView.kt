package mega.privacy.android.app.upgradeAccount.view

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UIAccountType
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.controls.MegaSpannedAlignedText
import mega.privacy.android.core.ui.controls.MegaSpannedText
import mega.privacy.android.core.ui.controls.SimpleNoTitleTopAppBar
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.core.ui.theme.body2
import mega.privacy.android.core.ui.theme.caption
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_800
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_050_white_alpha_050
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorBlackWhite
import mega.privacy.android.core.ui.theme.extensions.textColorPrimaryReverted
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.extensions.white_grey_alpha_087
import mega.privacy.android.core.ui.theme.subtitle1
import mega.privacy.android.core.ui.theme.subtitle2
import mega.privacy.android.core.ui.theme.teal_100
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.core.ui.theme.transparent
import java.util.Locale


@Composable
fun NewUpgradeAccountView(
    state: UpgradeAccountState,
    onBackPressed: () -> Unit,
    onPlanClicked: (AccountType) -> Unit,
    onTOSClicked: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var isMonthly by rememberSaveable { mutableStateOf(true) }
    var chosenPlan by rememberSaveable { mutableStateOf(AccountType.FREE) }
    Scaffold(topBar = {
        SimpleNoTitleTopAppBar(
            elevation = false,
            onBackPressed = onBackPressed
        )
    }) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(state = scrollState)
        ) {
            Text(
                text = "Choose the right plan for you",
                style = subtitle1,
                modifier = Modifier.padding(
                    start = 24.dp,
                    top = 8.dp,
                    bottom = 24.dp
                ),
                fontWeight = FontWeight.Medium,
            )
            MonthlyYearlyTabs(
                isMonthly = isMonthly
            ) { isMonthly = it }
            Box(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        top = 3.dp,
                        bottom = 16.dp
                    )
                    .background(
                        color = MaterialTheme.colors.grey_020_grey_800,
                        shape = RoundedCornerShape(100.dp)
                    )
            ) {
                Text(
                    text = "Save up to 16% with yearly billing",
                    style = subtitle2,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
                )
            }

            state.subscriptionsList.forEach {
                val isCurrentPlan = remember {
                    derivedStateOf { state.currentSubscriptionPlan == it.accountType }
                }
                val isRecommended = remember {
                    derivedStateOf { (((state.currentSubscriptionPlan == AccountType.FREE || state.currentSubscriptionPlan == AccountType.PRO_LITE) && it.accountType == AccountType.PRO_I) || (state.currentSubscriptionPlan == AccountType.PRO_I && it.accountType == AccountType.PRO_II) || (state.currentSubscriptionPlan == AccountType.PRO_II && it.accountType == AccountType.PRO_III)) }
                }
                SubscriptionPlansInfoRowNew(
                    proPlan = it.accountType,
                    subscription = it,
                    isCurrentPlan = isCurrentPlan.value,
                    isRecommended = isRecommended.value,
                    onPlanClicked = { chosenPlan = it.accountType },
                    chosenPlan = chosenPlan,
                    isMonthly = isMonthly,
                )
            }

            FeaturesOfPlans()

            if (chosenPlan != AccountType.FREE) {
                TextButton(
                    onClick = { onPlanClicked(chosenPlan) },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.teal_300_teal_200)
                ) {
                    val uiAccountType = mapUIAccountType(chosenPlan)
                    Text(
                        text = uiAccountType.textBuyAccountTypeValue,
                        style = MaterialTheme.typography.button,
                        color = MaterialTheme.colors.textColorPrimaryReverted,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.teal_300_teal_200,
                modifier = Modifier
                    .padding(
                        start = 24.dp,
                        top = 20.dp,
                        bottom = 16.dp
                    )
                    .testTag("TOS")
                    .clickable { onTOSClicked() },
                fontWeight = FontWeight.Medium,
            )
        }
    }
}


@Composable
fun MonthlyYearlyTabs(
    isMonthly: Boolean,
    onTabClicked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                bottom = 0.dp,
                top = 0.dp
            )
    ) {

        Button(
            onClick = { onTabClicked(true) },
            border = BorderStroke(
                width = 0.5.dp,
                color =
                if (isMonthly) transparent
                else MaterialTheme.colors.textColorSecondary
            ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =
                if (isMonthly) MaterialTheme.colors.teal_300_teal_200
                else transparent,
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            modifier = Modifier.padding(end = 8.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = if (isMonthly)
                PaddingValues(
                    start = 11.dp,
                    end = 16.dp
                )
            else PaddingValues(
                horizontal = 16.dp
            )
        ) {
            if (isMonthly) {
                Image(
                    painter = painterResource(R.drawable.ic_plans_montly_yearly_check),
                    contentDescription = "Check icon for monthly/yearly tabs, when selected",
                    modifier = Modifier
                        .padding(end = 11.dp)
                        .testTag("Monthly check"),
                )
            }
            Text(
                text = "Monthly",
                color =
                if (isMonthly) MaterialTheme.colors.white_grey_alpha_087
                else MaterialTheme.colors.textColorSecondary,
                style = body2,
                fontWeight = FontWeight.Medium,
            )
        }

        Button(
            onClick = { onTabClicked(false) },
            border = BorderStroke(
                width = 0.5.dp,
                color =
                if (isMonthly) MaterialTheme.colors.textColorSecondary
                else transparent,
            ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =
                if (isMonthly) transparent
                else MaterialTheme.colors.teal_300_teal_200
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            modifier = Modifier.padding(end = 8.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding =
            if (isMonthly)
                PaddingValues(
                    horizontal = 16.dp
                )
            else PaddingValues(
                start = 11.dp,
                end = 16.dp
            )
        ) {
            if (!isMonthly) {
                Image(
                    painter = painterResource(R.drawable.ic_plans_montly_yearly_check),
                    contentDescription = "Check icon for monthly/yearly tabs, when selected",
                    modifier = Modifier
                        .padding(end = 11.dp)
                        .testTag("Yearly check"),
                )
            }
            Text(
                text = "Yearly",
                color =
                if (isMonthly) MaterialTheme.colors.textColorSecondary
                else MaterialTheme.colors.white_grey_alpha_087,
                style = body2,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun SubscriptionPlansInfoRowNew(
    proPlan: AccountType,
    subscription: LocalisedSubscription,
    isCurrentPlan: Boolean,
    isRecommended: Boolean,
    onPlanClicked: () -> Unit,
    chosenPlan: AccountType,
    isMonthly: Boolean,
) {
    val isClicked = chosenPlan == proPlan

    val storageValueString =
        stringResource(
            id = subscription.formatStorageSize().first,
            subscription.formatStorageSize().second
        )
    val transferValueString =
        stringResource(
            id = subscription.formatTransferSize().first,
            subscription.formatTransferSize().second
        )

    val uiAccountType = mapUIAccountType(proPlan)

    val storageString = "[B]Storage:[/B] [A]$storageValueString[/A]"
    val transferString = "[B]Transfer:[/B] [A]$transferValueString[/A]"

    val formattedPrice = subscription.localisePriceCurrencyCode(Locale.getDefault())
    val priceString =
        if (isMonthly) "[A]${formattedPrice.first}[/A]\r\n[B]${formattedPrice.second}/month[/B]"
        else "[A]${formattedPrice.first}[/A]\r\n[B]${formattedPrice.second}/year[/B]"

    Card(shape = RoundedCornerShape(12.dp),
        elevation = if (isClicked) 8.dp else 0.dp,
        modifier = Modifier
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
            .testTag(stringResource(uiAccountType.textValue))) {
        Column {
            Row {
                Text(
                    text = stringResource(id = uiAccountType.textValue),
                    style = subtitle1,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 12.dp,
                        end = 8.dp,
                        bottom = 12.dp
                    ),
                    fontWeight = FontWeight.Medium,
                )
                if (isCurrentPlan) {
                    Text(
                        text = "Current plan",
                        style = subtitle2,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight(500),
                        color = MaterialTheme.colors.textColorBlackWhite,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.grey_050_grey_800,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterVertically)
                            .padding(
                                horizontal = 8.dp, vertical = 4.dp
                            )
                            .testTag("Current plan")
                    )
                }
                if (isRecommended) {
                    Text(
                        text = "Recommended",
                        style = subtitle2,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight(500),
                        color = black,
                        modifier = Modifier
                            .background(
                                color = teal_100,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterVertically)
                            .padding(
                                horizontal = 8.dp, vertical = 4.dp
                            )
                            .testTag("Recommended")
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
                            SpanIndicator('A') to SpanStyle(
                                color = MaterialTheme.colors.textColorBlackWhite,
                                fontWeight = FontWeight.Medium
                            ),
                            SpanIndicator('B') to SpanStyle(
                                color = MaterialTheme.colors.grey_alpha_050_white_alpha_050,
                            )
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    MegaSpannedText(
                        value = transferString,
                        baseStyle = body2,
                        styles = hashMapOf(
                            SpanIndicator('A') to SpanStyle(
                                color = MaterialTheme.colors.textColorBlackWhite,
                                fontWeight = FontWeight.Medium
                            ),
                            SpanIndicator('B') to SpanStyle(
                                color = MaterialTheme.colors.grey_alpha_050_white_alpha_050,
                            )
                        )
                    )
                }
                Column(
                    modifier = Modifier.weight(0.5f),
                    horizontalAlignment = Alignment.End
                ) {
                    MegaSpannedAlignedText(
                        value = priceString,
                        baseStyle = caption,
                        styles = hashMapOf(
                            SpanIndicator('A') to SpanStyle(
                                color = MaterialTheme.colors.textColorBlackWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(500),
                            ),
                            SpanIndicator('B') to SpanStyle(
                                color = MaterialTheme.colors.grey_alpha_050_white_alpha_050,
                            )
                        ),
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

@Composable
private fun FeaturesOfPlans() {
    Column(
        modifier = Modifier.padding(
            vertical = 12.dp,
            horizontal = 24.dp
        )
    ) {
        Text(
            text = "Features of Pro plans",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.grey_alpha_087_white_alpha_087,
            fontWeight = FontWeight.Medium,
        )
        Column(
            modifier = Modifier.padding(top = 16.dp)
        ) {

            Text(
                text = "•  Password protected links",
                style = body2,
                color = MaterialTheme.colors.textColorBlackWhite,
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            Text(
                text = "•  Links with expiry dates",
                style = body2,
                color = MaterialTheme.colors.textColorBlackWhite,
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            Text(
                text = "•  Transfer quota sharing",
                style = body2,
                color = MaterialTheme.colors.textColorBlackWhite,
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            Text(
                text = "•  Automatic backups",
                style = body2,
                color = MaterialTheme.colors.textColorBlackWhite,
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            Text(
                text = "•  Rewind up to 90 days on Pro Lite and up to 365 days on Pro I, II, and III",
                style = body2,
                color = MaterialTheme.colors.textColorBlackWhite,
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            Text(
                text = "•  Schedule rubbish bin clearing between 7 days to 10 years",
                style = body2,
                color = MaterialTheme.colors.textColorBlackWhite,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

private fun mapUIAccountType(plan: AccountType) = when (plan) {
    AccountType.PRO_I -> UIAccountType.PRO_I
    AccountType.PRO_II -> UIAccountType.PRO_II
    AccountType.PRO_III -> UIAccountType.PRO_III
    AccountType.PRO_LITE -> UIAccountType.PRO_LITE
    else -> UIAccountType.PRO_LITE
}


@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewUpgradeAccountViewNew() {
    val localisedPriceStringMapper = LocalisedPriceStringMapper()
    val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    val formattedSizeMapper = FormattedSizeMapper()
    val expectedSubscriptionsList: List<LocalisedSubscription>
    val subscriptionProIMonthly = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99.toFloat(), mega.privacy.android.domain.entity.Currency("EUR")),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    val subscriptionProIIMonthly = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyAmount(
            19.99.toFloat(),
            mega.privacy.android.domain.entity.Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,

    )

    val subscriptionProIIIMonthly = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyAmount(
            29.99.toFloat(),
            mega.privacy.android.domain.entity.Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    val subscriptionProLiteMonthly = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = 400,
        transfer = 1024,
        amount = CurrencyAmount(4.99.toFloat(), mega.privacy.android.domain.entity.Currency("NZD")),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    expectedSubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
    )
    MaterialTheme {
        NewUpgradeAccountView(
            state = UpgradeAccountState(
                subscriptionsList = expectedSubscriptionsList,
                currentSubscriptionPlan = AccountType.PRO_II,
                showBillingWarning = false,
                showBuyNewSubscriptionDialog = false,
                currentPayment = UpgradePayment(
                    upgradeType = Constants.INVALID_VALUE,
                    currentPayment = null,
                ),
            ),
            onBackPressed = {},
            onPlanClicked = {},
            onTOSClicked = {},
        )
    }
}
