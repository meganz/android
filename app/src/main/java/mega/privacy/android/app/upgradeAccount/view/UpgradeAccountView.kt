package mega.privacy.android.app.upgradeAccount.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.account.model.AccountStorageUIState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.model.UserSubscription
import mega.privacy.android.app.upgradeAccount.model.extensions.toUIAccountType
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.components.MonthlyYearlyTabs
import mega.privacy.android.app.upgradeAccount.view.components.ProPlanInfoCard
import mega.privacy.android.app.upgradeAccount.view.components.SaveUpToLabel
import mega.privacy.android.app.upgradeAccount.view.components.SubscriptionDetails
import mega.privacy.android.app.utils.Constants.PRICING_PAGE_URL
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.Typography
import mega.privacy.android.shared.original.core.ui.theme.extensions.accent_900_accent_050
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_yellow_700
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_100_alpha_060_dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.yellow_100_yellow_700_alpha_015
import mega.privacy.android.shared.original.core.ui.theme.subtitle1
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Screen is shown when user taps on Upgrade button
 * where user can view Pro plans and check the features available for these plans
 */
@Composable
fun UpgradeAccountView(
    state: UpgradeAccountState,
    accountStorageState: AccountStorageUIState,
    onBackPressed: () -> Unit,
    onBuyClicked: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onChoosingMonthlyYearlyPlan: (isMonthly: Boolean) -> Unit,
    onChoosingPlanType: (chosenPlan: AccountType) -> Unit,
    hideBillingWarning: () -> Unit,
    onDialogConfirmButtonClicked: (AccountType) -> Unit,
    onDialogDismissButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val userSubscription = state.userSubscription
    var isMonthly by rememberSaveable { mutableStateOf(userSubscription == UserSubscription.MONTHLY_SUBSCRIBED) }
    var chosenPlan by rememberSaveable { mutableStateOf(AccountType.FREE) }
    var isClickedCurrentPlan by rememberSaveable { mutableStateOf(false) }
    var isPreselectedPlanOnce by rememberSaveable { mutableStateOf(false) }
    val isPaymentMethodAvailable = state.isPaymentMethodAvailable
    var hideFloatButton by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    MegaScaffold(
        modifier = Modifier.systemBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = "",
                onNavigationPressed = { onBackPressed() },
                elevation = if (state.showBillingWarning) AppBarDefaults.TopAppBarElevation else 0.dp
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (!hideFloatButton && chosenPlan != AccountType.FREE) {
                val uiAccountType = chosenPlan.toUIAccountType()
                FloatingActionButton(
                    onClick = onBuyClicked,
                    content = {
                        Text(
                            text = stringResource(id = uiAccountType.textBuyButtonValue),
                            style = MaterialTheme.typography.button,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    backgroundColor = MaterialTheme.colors.accent_900_accent_050,
                    modifier = modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 30.dp
                        )
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("$BUY_BUTTON_TAG${uiAccountType.ordinal}"),
                    shape = RoundedCornerShape(4.dp),
                )
            }
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(state = scrollState)
        ) {
            if (state.showBillingWarning) {
                WarningBanner(hideBillingWarning = hideBillingWarning)
            }

            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_title_choose_right_plan),
                textColor = TextColor.Primary,
                style = subtitle1.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(
                    start = 24.dp,
                    top = 8.dp,
                    bottom = 24.dp
                ),
            )
            MonthlyYearlyTabs(
                isMonthly = isMonthly,
                onTabClicked = {
                    isMonthly = it
                    onChoosingMonthlyYearlyPlan(it)
                    hideFloatButton =
                        (isClickedCurrentPlan && ((isMonthly && userSubscription == UserSubscription.MONTHLY_SUBSCRIBED) || (!isMonthly && userSubscription == UserSubscription.YEARLY_SUBSCRIBED)))
                },
                testTag = UPGRADE_ACCOUNT_SCREEN_TAG,
            )
            SaveUpToLabel()

            if (state.localisedSubscriptionsList.isEmpty()) {
                LoadingShimmerEffect()
            } else {
                state.localisedSubscriptionsList.forEach {
                    val isCurrentPlan by remember {
                        derivedStateOf { state.currentSubscriptionPlan == it.accountType }
                    }
                    val isRecommended = remember {
                        derivedStateOf {
                            (((state.currentSubscriptionPlan == AccountType.STARTER ||
                                    state.currentSubscriptionPlan == AccountType.BASIC ||
                                    state.currentSubscriptionPlan == AccountType.ESSENTIAL ||
                                    state.currentSubscriptionPlan == AccountType.FREE ||
                                    state.currentSubscriptionPlan == AccountType.PRO_LITE) &&
                                    it.accountType == AccountType.PRO_I) ||
                                    (state.currentSubscriptionPlan == AccountType.PRO_I &&
                                            it.accountType == AccountType.PRO_II) ||
                                    (state.currentSubscriptionPlan == AccountType.PRO_II &&
                                            it.accountType == AccountType.PRO_III))
                        }
                    }
                    val showCurrentPlanLabel =
                        isCurrentPlan && ((userSubscription == UserSubscription.NOT_SUBSCRIBED) || (isMonthly && userSubscription == UserSubscription.MONTHLY_SUBSCRIBED) || (!isMonthly && userSubscription == UserSubscription.YEARLY_SUBSCRIBED))
                    val disableCardClick =
                        isCurrentPlan && ((isMonthly && userSubscription == UserSubscription.MONTHLY_SUBSCRIBED) || (!isMonthly && userSubscription == UserSubscription.YEARLY_SUBSCRIBED))
                    val isClicked =
                        (chosenPlan == it.accountType) && isPaymentMethodAvailable && !disableCardClick
                    if (isRecommended.value && !isPreselectedPlanOnce && isPaymentMethodAvailable) {
                        chosenPlan = it.accountType
                        onChoosingMonthlyYearlyPlan(isMonthly)
                        onChoosingPlanType(it.accountType)
                    }
                    ProPlanInfoCard(
                        proPlan = it.accountType,
                        baseStorageFormatted = accountStorageState.baseStorageFormatted,
                        subscription = it,
                        isRecommended = isRecommended.value,
                        onPlanClicked = {
                            if (!isPaymentMethodAvailable) {
                                chosenPlan = AccountType.FREE
                                onChoosingPlanType(it.accountType)
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                            }
                            if (isPaymentMethodAvailable) {
                                if (disableCardClick) {
                                    coroutineScope.launch {
                                        scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                                            message = context.getString(R.string.account_upgrade_account_snackbar_recurring_plan_already_exist)
                                        )
                                    }
                                } else {
                                    chosenPlan = it.accountType
                                    isPreselectedPlanOnce = true
                                    onChoosingMonthlyYearlyPlan(isMonthly)
                                    onChoosingPlanType(it.accountType)
                                    isClickedCurrentPlan = isCurrentPlan
                                    hideFloatButton = false
                                }
                            }
                        },
                        isMonthly = isMonthly,
                        isClicked = isClicked,
                        showCurrentPlanLabel = showCurrentPlanLabel,
                        testTag = PRO_PLAN_CARD_TAG,
                    )
                }

                if (state.currentSubscriptionPlan == AccountType.PRO_III) {
                    PricingPageLinkText(
                        onLinkClick = onLinkClicked
                    )
                }

                FeaturesOfPlans(
                    showNoAdsFeature = state.showNoAdsFeature,
                )
                if (state.localisedSubscriptionsList.isNotEmpty()) {
                    SubscriptionDetails(
                        onLinkClick = onLinkClicked,
                        chosenPlan = chosenPlan,
                        subscriptionList = state.localisedSubscriptionsList,
                        isMonthly = isMonthly,
                    )
                }
            }
        }
    }
    if (state.showBuyNewSubscriptionDialog) {
        BuyNewSubscriptionDialog(
            upgradeType = state.currentPayment.upgradeType,
            paymentMethod = state.currentPayment.currentPayment ?: return,
            onDialogPositiveButtonClicked = onDialogConfirmButtonClicked,
            onDialogDismissButtonClicked = { onDialogDismissButtonClicked() }
        )
    }
}

@Composable
fun WarningBanner(hideBillingWarning: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colors.yellow_100_yellow_700_alpha_015
            )
            .testTag(BILLING_WARNING_TAG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(.9f)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    )
            ) {
                Text(
                    text = stringResource(id = R.string.upgrade_billing_warning),
                    fontSize = 13.sp,
                    style = Typography.caption,
                    color = MaterialTheme.colors.black_yellow_700,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(.1f)
                    .padding(
                        top = 0.dp,
                        end = 17.dp
                    )
            ) {
                IconButton(
                    onClick = hideBillingWarning,
                    modifier = Modifier.testTag(BILLING_WARNING_CLOSE_BUTTON_TAG)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_remove_billing_warning),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingShimmerEffect() {
    val gradient = listOf(
        MaterialTheme.colors.grey_100_alpha_060_dark_grey,
        MaterialTheme.colors.grey_020_grey_800,
        MaterialTheme.colors.grey_100_alpha_060_dark_grey,
    )

    val transition = rememberInfiniteTransition(label = "upgrade_account_screen_shimmer")

    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutLinearInEasing
            )
        ),
        label = "upgrade_account_screen_shimmer"
    )
    val brush = linearGradient(
        colors = gradient,
        start = Offset.Zero,
        end = Offset(
            x = translateAnimation.value,
            y = translateAnimation.value
        )
    )
    EmptySubscriptionPlansInfoCards(brush = brush)
}

@Composable
fun EmptySubscriptionPlansInfoCards(brush: Brush) {
    for (i in 1..4) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            modifier = Modifier
                .padding(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                    shape = RoundedCornerShape(12.dp),
                )
                .testTag(EMPTY_CARD_TAG)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .height(45.dp)
                        .padding(start = 16.dp)
                ) {
                    Spacer(
                        modifier = Modifier
                            .height(20.dp)
                            .width(100.dp)
                            .background(brush)
                            .align(Alignment.CenterVertically)
                    )
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
                        Row {
                            Spacer(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(56.dp)
                                    .padding(
                                        end = 8.dp,
                                        bottom = 8.dp
                                    )
                                    .background(brush)
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(32.dp)
                                    .padding(
                                        end = 8.dp,
                                        bottom = 8.dp
                                    )
                                    .background(brush)
                            )
                        }
                        Row {
                            Spacer(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(64.dp)
                                    .padding(end = 8.dp)
                                    .background(brush)
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(32.dp)
                                    .background(brush)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.5f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Spacer(
                            modifier = Modifier
                                .height(25.dp)
                                .width(64.dp)
                                .padding(bottom = 5.dp)
                                .background(brush)
                        )
                        Row {
                            Spacer(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(48.dp)
                                    .padding(end = 2.dp)
                                    .background(brush)
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(24.dp)
                                    .background(brush)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturesOfPlans(
    showNoAdsFeature: Boolean,
) {
    val modifier = Modifier.padding(
        start = 10.dp,
        bottom = 12.dp
    )
    val textIndent = TextIndent(restLine = 11.sp)
    Column(
        modifier = Modifier.padding(
            start = 24.dp,
            top = 12.dp,
            end = 24.dp,
            bottom = 30.dp
        )
    ) {
        MegaText(
            text = stringResource(id = R.string.account_upgrade_account_description_title_features),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1medium,
        )
        Column(
            modifier = Modifier.padding(top = 16.dp)
        ) {
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_password),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = modifier
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_link),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = modifier
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_transfer),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = modifier
            )
            MegaText(
                text = stringResource(id = sharedR.string.account_upgrade_account_description_feature_sync),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = modifier
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_rewind),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = modifier
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_meetings),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = modifier
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_vpn),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_feature_rubbish_bin),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = 12.dp
                )
            )
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_description_priority_support),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                modifier = Modifier.padding(
                    start = 10.dp,
                    bottom = if (showNoAdsFeature) 12.dp else 0.dp
                )
            )
            if (showNoAdsFeature) {
                MegaText(
                    text = stringResource(id = R.string.account_upgrade_account_description_feature_advertisement),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.body2.copy(textIndent = textIndent),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun PricingPageLinkText(
    onLinkClick: (link: String) -> Unit,
) {
    MegaSpannedClickableText(
        modifier = Modifier
            .padding(16.dp)
            .testTag(PRICING_PAGE_LINK_TAG),
        value = stringResource(
            id = R.string.account_upgrade_account_pricing_page_link_text
        ),
        styles = hashMapOf(
            SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                MegaSpanStyle(
                    SpanStyle(fontWeight = FontWeight.W500),
                    color = TextColor.Accent,
                ),
                PRICING_PAGE_URL
            )
        ),
        onAnnotationClick = onLinkClick,
        baseStyle = MaterialTheme.typography.subtitle2,
        color = TextColor.Primary
    )
}


@CombinedThemePreviews
@Composable
fun PreviewUpgradeAccountView(
    @PreviewParameter(UpgradeAccountPreviewProvider::class) state: UpgradeAccountState,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        UpgradeAccountView(
            state = state,
            accountStorageState = AccountStorageUIState(),
            onBackPressed = {},
            onBuyClicked = {},
            onLinkClicked = {},
            onChoosingMonthlyYearlyPlan = {},
            onChoosingPlanType = {},
            hideBillingWarning = {},
            onDialogConfirmButtonClicked = {},
            onDialogDismissButtonClicked = {},
        )
    }
}

private class UpgradeAccountPreviewProvider :
    PreviewParameterProvider<UpgradeAccountState> {
    val localisedSubscriptionsList: List<LocalisedSubscription> = listOf(
        subscriptionProLite,
        subscriptionProI,
        subscriptionProII,
        subscriptionProIII
    )

    override val values: Sequence<UpgradeAccountState>
        get() = sequenceOf(
            UpgradeAccountState(
                localisedSubscriptionsList = localisedSubscriptionsList,
                currentSubscriptionPlan = AccountType.FREE,
                showBillingWarning = false,
                showBuyNewSubscriptionDialog = false,
                currentPayment = UpgradePayment(
                    upgradeType = AccountType.UNKNOWN,
                    currentPayment = null,
                ),
            )
        )

    companion object {
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

        val subscriptionProII = LocalisedSubscription(
            accountType = AccountType.PRO_II,
            storage = 8192,
            monthlyTransfer = 8192,
            yearlyTransfer = 98304,
            monthlyAmount = CurrencyAmount(19.99.toFloat(), Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                199.99.toFloat(),
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProIII = LocalisedSubscription(
            accountType = AccountType.PRO_III,
            storage = 16384,
            monthlyTransfer = 16384,
            yearlyTransfer = 196608,
            monthlyAmount = CurrencyAmount(29.99.toFloat(), Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                299.99.toFloat(),
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProLite = LocalisedSubscription(
            accountType = AccountType.PRO_LITE,
            storage = 400,
            monthlyTransfer = 1024,
            yearlyTransfer = 12288,
            monthlyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                49.99.toFloat(),
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )
    }
}

internal const val UPGRADE_ACCOUNT_SCREEN_TAG = "upgrade_account_screen:"
internal const val BILLING_WARNING_TAG = "upgrade_account_screen:box_warning_unavailable_payments"
internal const val BILLING_WARNING_CLOSE_BUTTON_TAG =
    "upgrade_account_screen:button_close_billing_warning"
internal const val EMPTY_CARD_TAG = "upgrade_account_screen:card_empty_loading_plans"
internal const val PRO_PLAN_CARD_TAG = "upgrade_account_screen:card_pro_plan_"
internal const val PRICING_PAGE_LINK_TAG = "upgrade_account_screen:text_pricing_page_link"
internal const val BUY_BUTTON_TAG = "upgrade_account_screen:button_buy_pro_plan_"
internal const val UPGRADE_WARNING_BANNER_TAG = "upgrade_account_screen:upgrade_warning_banner"
internal const val GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG =
    "upgrade_account_screen:text_link_google_play_store_subscription"
internal const val SUBSCRIPTION_DETAILS_TITLE_TAG =
    "upgrade_account_screen:text_subscription_details_title"
internal const val SUBSCRIPTION_DETAILS_DESCRIPTION_TAG =
    "upgrade_account_screen:text_subscription_details_description"

