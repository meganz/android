package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.R
import mega.privacy.android.feature.payment.components.AdditionalBenefitProPlanView
import mega.privacy.android.feature.payment.components.BuyPlanBottomBar
import mega.privacy.android.feature.payment.components.ChooseAccountScreenTopBar
import mega.privacy.android.feature.payment.components.FreePlanCard
import mega.privacy.android.feature.payment.components.NewFeatureRow
import mega.privacy.android.feature.payment.components.ProPlanCard
import mega.privacy.android.feature.payment.components.TEST_TAG_FREE_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_PRO_PLAN_CARD
import mega.privacy.android.feature.payment.model.AccountStorageUIState
import mega.privacy.android.feature.payment.model.ChooseAccountState
import mega.privacy.android.feature.payment.model.ProFeature
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale


@Composable
fun NewChooseAccountScreen(
    onBuyPlanClick: (AccountType, Boolean, String?) -> Unit,
    maybeLaterClicked: () -> Unit,
    onFreePlanClicked: () -> Unit,
    uiState: ChooseAccountState = ChooseAccountState(),
    accountStorageUiState: AccountStorageUIState = AccountStorageUIState(),
    isNewCreationAccount: Boolean = false,
    isUpgradeAccount: Boolean = false,
    onBack: () -> Unit,
) {
    var chosenPlan by rememberSaveable { mutableStateOf<AccountType?>(null) }
    var isMonthly by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = Locale.getDefault()

    val lazyListState = rememberLazyListState()
    val topBarHeightPx =
        with(LocalDensity.current) { 56.dp.roundToPx() + WindowInsets.statusBars.getTop(this) }
    val headerHeightPx = with(LocalDensity.current) { 180.dp.roundToPx() }
    val position by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val itemOffset by remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }
    val currentHeaderHeightPx = headerHeightPx - itemOffset
    val transparent = position == 0 && currentHeaderHeightPx > topBarHeightPx
    val alpha by animateFloatAsState(targetValue = if (transparent) 0f else 1f)

    // Compute highest storage capacity among available subscriptions
    val highestStorageSubscription = uiState.localisedSubscriptionsList.maxByOrNull { it.storage }
    val highestStorageString = if (highestStorageSubscription != null) {
        val formattedSize = highestStorageSubscription.formatStorageSize()
        stringResource(id = formattedSize.unit, formattedSize.size)
    } else {
        "20 TB"
    }

    val baseStorageFormatted = remember(accountStorageUiState.baseStorage) {
        accountStorageUiState.baseStorage?.let {
            formatFileSize(it, context)
        }.orEmpty()
    }

    val hasDiscount = remember(uiState) { uiState.hasDiscount() }

    LaunchedEffect(uiState.localisedSubscriptionsList, uiState.currentSubscriptionPlan) {
        if (chosenPlan == null) {
            uiState.localisedSubscriptionsList.find { it.hasDiscount }?.let {
                chosenPlan = it.accountType
            }
        }
    }

    val proFeatures = remember(highestStorageString) {
        listOf(
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.Cloud,
                title = context.getString(sharedR.string.pro_plan_feature_storage_title),
                description = context.getString(
                    sharedR.string.pro_plan_feature_storage_desc,
                    highestStorageString
                ),
                testTag = "pro_plan:feature:storage"
            ),
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
                title = context.getString(sharedR.string.pro_plan_feature_transfer_title),
                description = context.getString(sharedR.string.pro_plan_feature_transfer_desc),
                testTag = "pro_plan:feature:transfer"
            ),
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.VPN,
                title = context.getString(sharedR.string.pro_plan_feature_vpn_title),
                description = context.getString(sharedR.string.pro_plan_feature_vpn_desc),
                testTag = "pro_plan:feature:vpn"
            ),
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.LockKeyholeCircle,
                title = context.getString(sharedR.string.pro_plan_feature_pass_title),
                description = context.getString(sharedR.string.pro_plan_feature_pass_desc),
                testTag = "pro_plan:feature:pass"
            )
        )
    }

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            ChooseAccountScreenTopBar(
                alpha = alpha,
                isUpgradeAccount = isUpgradeAccount,
                maybeLaterClicked = maybeLaterClicked,
                onBack = onBack
            )
        },
        bottomBar = {
            chosenPlan?.takeIf {
                !isCurrentPlan(
                    uiState = uiState,
                    subscriptionAccountType = it,
                    isMonthly = isMonthly,
                    isUpgradeAccount = isUpgradeAccount
                )
            }?.let {
                BuyPlanBottomBar(
                    modifier = Modifier,
                    text = stringResource(it.toUIAccountType().textBuyButtonValue),
                    onClick = {
                        val offerId =
                            uiState.localisedSubscriptionsList
                                .find { sub -> sub.accountType == chosenPlan }
                                ?.getOfferId(isMonthly)
                        onBuyPlanClick(it, isMonthly, offerId)
                    },
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .testTag(TEST_TAG_LAZY_COLUMN)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize(),
            state = lazyListState,
        ) {
            item("image_header") {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag(TEST_TAG_IMAGE_HEADER),
                    painter = painterResource(IconPackR.drawable.choose_account_type_header),
                    contentDescription = "Header Image",
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item("get_more_with_pro_plan") {
                MegaText(
                    text = stringResource(sharedR.string.choose_account_screen_get_more_with_pro_plan_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textColor = TextColor.Primary,
                    modifier = Modifier
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                        .testTag(TEST_TAG_TITLE)
                )
                MegaText(
                    text = stringResource(id = sharedR.string.pro_plan_features_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    textColor = TextColor.Primary,
                    modifier = Modifier
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                        .testTag(TEST_TAG_FEATURES_SECTION_TITLE)
                )
            }
            items(proFeatures, key = { it.title }) { feature ->
                val index = proFeatures.indexOf(feature)
                NewFeatureRow(
                    painter = rememberVectorPainter(feature.icon),
                    title = feature.title,
                    description = feature.description,
                    testTag = feature.testTag,
                    modifier = Modifier.testTag("$TEST_TAG_FEATURE_ROW$index")
                )
            }

            item("subscription_period") {
                Row(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MegaChip(
                        modifier = Modifier.testTag(TEST_TAG_MONTHLY_CHIP),
                        selected = isMonthly,
                        onClick = {
                            isMonthly = true
                        },
                        content = stringResource(id = R.string.subscription_type_monthly),
                        leadingPainter = if (isMonthly) {
                            rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
                        } else null
                    )

                    MegaChip(
                        modifier = Modifier.testTag(TEST_TAG_YEARLY_CHIP),
                        selected = !isMonthly,
                        onClick = {
                            isMonthly = false
                        },
                        content = stringResource(id = R.string.subscription_type_yearly),
                        leadingPainter = if (!isMonthly) {
                            rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
                        } else null
                    )
                }
            }

            item("save_up_to_badge_${hasDiscount}") {
                val label = if (hasDiscount) {
                    stringResource(sharedR.string.account_upgrade_account_label_save_at_least)
                } else {
                    stringResource(sharedR.string.account_upgrade_account_label_save_up_to)
                }
                val badgeType = if (hasDiscount) {
                    BadgeType.MegaSecondary
                } else {
                    BadgeType.Mega
                }
                Badge(
                    badgeType = badgeType,
                    text = label,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(TEST_TAG_SAVE_UP_TO_BADGE)
                )
            }

            item("pro_plans_top_space") {
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(uiState.localisedSubscriptionsList) { index, subscription ->
                val isRecommended = !hasDiscount
                        && uiState.cheapestSubscriptionAvailable?.accountType == subscription.accountType
                val storageFormattedSize = subscription.formatStorageSize()
                val transferFormattedSize = subscription.formatTransferSize(isMonthly)

                val uiAccountType = subscription.accountType.toUIAccountType()

                val storageString = stringResource(
                    id = sharedR.string.choose_account_screen_storage_label,
                    stringResource(id = storageFormattedSize.unit, storageFormattedSize.size)
                )
                val transferString = stringResource(
                    id = sharedR.string.choose_account_screen_transfer_quota_label,
                    stringResource(id = transferFormattedSize.unit, transferFormattedSize.size)
                )
                val totalPrice =
                    subscription.localisePriceCurrencyCode(locale, isMonthly)

                val yearlyPricePerMonth = if (!isMonthly) {
                    subscription.localisePriceOfYearlyAmountPerMonth(locale)
                } else null

                val currentSubscription =
                    if (isMonthly) subscription.monthlySubscription else subscription.yearlySubscription
                val discountPercentage = currentSubscription.discountedPercentage
                val offerPeriod = currentSubscription.offerPeriod
                val discountedPriceMonthly =
                    subscription.localiseDiscountedPriceMonthlyCurrencyCode(locale, isMonthly)
                val discountedPriceYearly =
                    subscription.localiseDiscountedPriceYearlyCurrencyCode(locale, isMonthly)

                // in case subscriptionCycle is UNKNOWN and currentSubscriptionPlan is PRO level, we show it as current plan for both monthly and yearly
                val isCurrentPlan = isCurrentPlan(
                    uiState = uiState,
                    subscriptionAccountType = subscription.accountType,
                    isMonthly = isMonthly,
                    isUpgradeAccount = isUpgradeAccount
                )

                val billingInfo = if (!isMonthly) {
                    if (!isCurrentPlan && discountedPriceYearly != null
                        && discountPercentage != null && offerPeriod != null
                    ) {
                        "[A]${totalPrice.price}[/A] ${
                            getOfferPeriodLabel(
                                discountedPriceYearly.price,
                                offerPeriod
                            )
                        }"
                    } else {
                        stringResource(
                            sharedR.string.choose_account_screen_billed_yearly,
                            totalPrice.price
                        )
                    }
                } else null

                ProPlanCard(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .testTag("$TEST_TAG_PRO_PLAN_CARD$index"),
                    planName = stringResource(id = uiAccountType.textValue),
                    isRecommended = isRecommended,
                    isSelected = chosenPlan == subscription.accountType && !isCurrentPlan,
                    storage = storageString,
                    transfer = transferString,
                    price = yearlyPricePerMonth?.price ?: totalPrice.price,
                    billingInfo = billingInfo,
                    offerName = discountPercentage?.takeIf { !isCurrentPlan }?.let {
                        getCampaignName(
                            context = context,
                            offerId = currentSubscription.offerId,
                            discountPercentage = discountPercentage
                        )
                    },
                    discountedPrice = discountedPriceMonthly?.price?.takeIf { !isCurrentPlan },
                    isCurrentPlan = isCurrentPlan,
                    onSelected = {
                        chosenPlan = subscription.accountType
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            item("additional_benefits") {
                AdditionalBenefitProPlanView(
                    title = stringResource(id = sharedR.string.pro_plan_additional_benefits_section_title),
                    benefits = listOf(
                        stringResource(id = sharedR.string.pro_plan_benefit_password_protected_links),
                        stringResource(id = sharedR.string.pro_plan_benefit_links_with_expiry_dates),
                        stringResource(id = sharedR.string.pro_plan_benefit_auto_sync_mobile),
                        stringResource(id = sharedR.string.pro_plan_benefit_rewind_days, 60),
                        stringResource(id = sharedR.string.pro_plan_benefit_host_calls_unlimited),
                        stringResource(id = sharedR.string.pro_plan_benefit_schedule_rubbish_bin_clearing),
                        stringResource(id = sharedR.string.pro_plan_benefit_priority_support),
                    ),
                    modifier = Modifier.testTag(TEST_TAG_ADDITIONAL_BENEFITS)
                )
            }

            item("free_plan_card") {
                if (!isUpgradeAccount) {
                    FreePlanCard(
                        modifier = Modifier
                            .padding(16.dp)
                            .testTag(TEST_TAG_FREE_PLAN_CARD),
                        onContinue = onFreePlanClicked,
                        isNewCreationAccount = isNewCreationAccount,
                        storageFormatted = baseStorageFormatted,
                    )
                }
            }

            item("subscription_info") {
                SubscriptionInformation(context)
            }
        }
    }
}

fun isCurrentPlan(
    uiState: ChooseAccountState,
    subscriptionAccountType: AccountType,
    isMonthly: Boolean,
    isUpgradeAccount: Boolean,
): Boolean = uiState.currentSubscriptionPlan == subscriptionAccountType
        && isUpgradeAccount
        && (uiState.subscriptionCycle == AccountSubscriptionCycle.UNKNOWN
        || (isMonthly && uiState.subscriptionCycle == AccountSubscriptionCycle.MONTHLY)
        || (!isMonthly && uiState.subscriptionCycle == AccountSubscriptionCycle.YEARLY))

@Composable
fun getOfferPeriodLabel(discountedPrice: String, period: OfferPeriod) = when (period) {
    is OfferPeriod.Month -> pluralStringResource(
        id = sharedR.plurals.label_first_time_in_months_full,
        period.value,
        discountedPrice,
        period.value
    )

    is OfferPeriod.Year -> pluralStringResource(
        id = sharedR.plurals.label_first_time_in_years_full,
        period.value,
        discountedPrice,
        period.value
    )
}

fun getCampaignName(context: Context, offerId: String?, discountPercentage: Int): String =
    when (offerId) {
        CAMPAIGN_BLACK_FRIDAY -> context.getString(
            sharedR.string.campaign_name_black_friday,
            discountPercentage
        )

        CAMPAIGN_CYBER_MONDAY -> context.getString(
            sharedR.string.campaign_name_cyber_monday,
            discountPercentage
        )

        else -> context.getString(sharedR.string.campaign_name_special_offer, discountPercentage)
    }

@CombinedThemePreviews
@Composable
internal fun NewChooseAccountScreenPreview(
    @PreviewParameter(ChooseAccountPreviewProvider::class) state: ChooseAccountState,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        NewChooseAccountScreen(
            uiState = state,
            accountStorageUiState = AccountStorageUIState(
                baseStorage = 15L * 1024 * 1024 * 1024,
                totalStorage = 100L * 1024 * 1024 * 1024,
            ),
            isNewCreationAccount = false,
            isUpgradeAccount = false,
            onBuyPlanClick = { _, _, _ -> },
            onFreePlanClicked = {},
            maybeLaterClicked = {},
            onBack = {}
        )
    }
}

private const val CAMPAIGN_BLACK_FRIDAY = "black-friday"
private const val CAMPAIGN_CYBER_MONDAY = "cyber-monday"

/**
 * Test tag for the yearly chip selector
 */
internal const val TEST_TAG_YEARLY_CHIP = "choose_account_screen:yearly_chip"

/**
 * Test tag for the monthly chip selector
 */
internal const val TEST_TAG_MONTHLY_CHIP = "choose_account_screen:monthly_chip"

/**
 * Test tag for the header image at the top of the screen
 */
internal const val TEST_TAG_IMAGE_HEADER = "choose_account_screen:image_header"

/**
 * Test tag for the main title ("Get more with Pro plan")
 */
internal const val TEST_TAG_TITLE = "choose_account_screen:title"

/**
 * Test tag for the features section title ("You'll get:")
 */
internal const val TEST_TAG_FEATURES_SECTION_TITLE = "choose_account_screen:features_section_title"

/**
 * Test tag prefix for each Pro feature row (append index)
 */
internal const val TEST_TAG_FEATURE_ROW = "choose_account_screen:feature_row_"

/**
 * Test tag for the "Save up to" badge
 */
internal const val TEST_TAG_SAVE_UP_TO_BADGE = "choose_account_screen:save_up_to_badge"

/**
 * Test tag for the additional benefits section
 */
internal const val TEST_TAG_ADDITIONAL_BENEFITS = "choose_account_screen:additional_benefits"

/**
 * Test tag for the subscription info title
 */
internal const val TEST_TAG_SUBSCRIPTION_INFO_TITLE =
    "choose_account_screen:subscription_info_title"

/**
 * Test tag for the subscription info description
 */
internal const val TEST_TAG_SUBSCRIPTION_INFO_DESC = "choose_account_screen:subscription_info_desc"

/**
 * Test tag for the terms and policies section
 */
internal const val TEST_TAG_TERMS_AND_POLICIES = "choose_account_screen:terms_and_policies"

/**
 * Test tag for lazy column
 */
internal const val TEST_TAG_LAZY_COLUMN = "choose_account_screen:lazy_column"
