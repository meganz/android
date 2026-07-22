package mega.privacy.android.feature.payment.presentation.quotawarning

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.AnchoredButtonGroup
import mega.android.core.ui.components.button.SecondaryNavigationIconButton
import mega.android.core.ui.model.Button
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.feature.payment.components.QuotaCurrentPlanCard
import mega.privacy.android.feature.payment.components.QuotaOfferPlanCard
import mega.privacy.android.feature.payment.components.QuotaRecommendedPlanCard
import mega.privacy.android.feature.payment.components.QuotaUsageLevel
import mega.privacy.android.feature.payment.components.QuotaWarningSkeleton
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.feature.payment.presentation.upgrade.billedDescription
import mega.privacy.android.feature.payment.presentation.upgrade.getCampaignName
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale

/**
 * Quota-warning upsell screen: shows the user's current usage against the plan limit and
 * recommends the next-tier plan to upgrade to. The exact copy and metric depend on [type].
 *
 * @param onUpgradeClick called with the subscription to purchase when the upgrade button is tapped
 * @param onViewAllPlansClick called when the "View all plans" link is tapped
 * @param onLearnMoreClick called when the "Learn more" link (transfer scenario) is tapped
 * @param onContactSupportClick called when the "Contact support" button (highest-plan scenario) is tapped
 * @param onManagePlanClick called when the inline "mega.io" link (highest-plan scenario) is tapped
 * @param onClose called when the close button is tapped
 */
@Composable
fun QuotaWarningUpgradeScreen(
    type: QuotaWarningType,
    trigger: QuotaWarningTrigger,
    uiState: QuotaWarningUpgradeState,
    onUpgradeClick: (Subscription) -> Unit,
    onViewAllPlansClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onManagePlanClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val isProUser = uiState.currentPlan != null && uiState.currentPlan != AccountType.FREE
    val message = remember(
        type,
        trigger,
        uiState.storageState,
        uiState.isTransferOverQuota,
        isProUser,
        uiState.isHighestPlan,
    ) {
        QuotaWarningMessageMapper()(
            type = type,
            trigger = trigger,
            storageState = uiState.storageState,
            isTransferOverQuota = uiState.isTransferOverQuota,
            isProUser = isProUser,
            isHighestPlan = uiState.isHighestPlan,
        )
    }

    val title = if (message.titleTakesPercentage) {
        stringResource(message.titleId, currentUsagePercentage(uiState, message.metric).toInt())
    } else {
        stringResource(message.titleId)
    }
    val subtitle = stringResource(message.subtitleId)

    val currentCard = CurrentCardData(
        planName = stringResource(
            (uiState.currentPlan ?: AccountType.FREE).toUIAccountType().textValue
        ),
        currentPlanLabel = stringResource(sharedR.string.account_upgrade_account_pro_plan_info_current_plan_label),
        usagePercentage = currentUsagePercentage(uiState, message.metric),
        usageLevel = message.level,
        usageText = currentUsageText(uiState, message.metric, isProUser, context),
    )

    val recommended = uiState.recommendedSubscription?.let {
        recommendedCardData(it, uiState, message.metric, locale, context)
    }

    QuotaWarningUpgradeContent(
        illustrationRes = IconPackR.drawable.illustration_mega_secondary_quota_warning,
        title = title,
        subtitle = subtitle,
        showLearnMore = message.showLearnMore,
        subtitleHasLink = message.subtitleHasLink,
        isHighestPlan = uiState.isHighestPlan,
        isLoading = uiState.isLoading,
        currentCard = currentCard,
        recommended = recommended,
        onUpgradeClick = onUpgradeClick,
        onViewAllPlansClick = onViewAllPlansClick,
        onLearnMoreClick = onLearnMoreClick,
        onContactSupportClick = onContactSupportClick,
        onManagePlanClick = onManagePlanClick,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun QuotaWarningUpgradeContent(
    illustrationRes: Int,
    title: String,
    subtitle: String,
    showLearnMore: Boolean,
    subtitleHasLink: Boolean,
    isHighestPlan: Boolean,
    isLoading: Boolean,
    currentCard: CurrentCardData,
    recommended: RecommendedCardData?,
    onUpgradeClick: (Subscription) -> Unit,
    onViewAllPlansClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onManagePlanClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
            ) {
                SecondaryNavigationIconButton(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag(TEST_TAG_QUOTA_WARNING_CLOSE),
                    icon = rememberVectorPainter(
                        mega.privacy.android.icon.pack.IconPack.Medium.Thin.Outline.X
                    ),
                    onClick = onClose,
                )
            }
        },
        bottomBar = {
            if (!isLoading) {
                QuotaWarningBottomBar(
                    recommended = recommended,
                    isHighestPlan = isHighestPlan,
                    onUpgradeClick = onUpgradeClick,
                    onViewAllPlansClick = onViewAllPlansClick,
                    onContactSupportClick = onContactSupportClick,
                )
            }
        },
    ) { innerPadding ->
        if (isLoading) {
            QuotaWarningLoadingContent(modifier = Modifier.padding(innerPadding))
        } else {
            QuotaWarningDataContent(
                illustrationRes = illustrationRes,
                title = title,
                subtitle = subtitle,
                showLearnMore = showLearnMore,
                subtitleHasLink = subtitleHasLink,
                currentCard = currentCard,
                recommended = recommended,
                onLearnMoreClick = onLearnMoreClick,
                onManagePlanClick = onManagePlanClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun QuotaWarningLoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QuotaWarningSkeleton(
            modifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag(TEST_TAG_QUOTA_WARNING_SKELETON),
        )
    }
}

@Composable
private fun QuotaWarningDataContent(
    illustrationRes: Int,
    title: String,
    subtitle: String,
    showLearnMore: Boolean,
    subtitleHasLink: Boolean,
    currentCard: CurrentCardData,
    recommended: RecommendedCardData?,
    onLearnMoreClick: () -> Unit,
    onManagePlanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(illustrationRes),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .testTag(TEST_TAG_QUOTA_WARNING_ILLUSTRATION),
            )
            MegaText(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEST_TAG_QUOTA_WARNING_TITLE),
            )
            if (subtitleHasLink) {
                LinkSpannedText(
                    value = subtitle,
                    spanStyles = mapOf(
                        SpanIndicator('A') to SpanStyleWithAnnotation(
                            megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                                spanStyle = SpanStyle(),
                                linkColor = LinkColor.Primary,
                            ),
                            annotation = MANAGE_PLAN_LINK_ANNOTATION,
                        ),
                    ),
                    onAnnotationClick = { onManagePlanClick() },
                    baseTextColor = TextColor.Primary,
                    baseStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_QUOTA_WARNING_SUBTITLE),
                )
            } else {
                MegaText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textColor = TextColor.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_QUOTA_WARNING_SUBTITLE),
                )
            }
            if (showLearnMore) {
                MegaText(
                    text = stringResource(sharedR.string.general_learn_more),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                    ),
                    textColor = TextColor.Primary,
                    modifier = Modifier
                        .clickable(onClick = onLearnMoreClick)
                        .testTag(TEST_TAG_QUOTA_WARNING_LEARN_MORE),
                )
            }
            QuotaCurrentPlanCard(
                planName = currentCard.planName,
                currentPlanLabel = currentCard.currentPlanLabel,
                usagePercentage = currentCard.usagePercentage,
                usageLevel = currentCard.usageLevel,
                usageText = currentCard.usageText,
            )
            if (recommended != null) {
                val offer = recommended.offer
                if (offer != null) {
                    QuotaOfferPlanCard(
                        planName = recommended.planName,
                        priceText = offer.priceText,
                        originalPriceText = offer.originalPriceText,
                        discountDescriptionText = offer.discountDescriptionText,
                        discountBadgeText = offer.discountBadgeText,
                        storageText = recommended.storageText,
                        transferText = recommended.transferText,
                        usagePercentage = recommended.usagePercentage,
                        usageLevel = recommended.usageLevel,
                        usageText = recommended.usageText,
                        monthlyPriceText = offer.monthlyPriceText,
                    )
                } else {
                    QuotaRecommendedPlanCard(
                        planName = recommended.planName,
                        monthlyPriceText = recommended.monthlyPriceText,
                        yearlyTotalText = recommended.yearlyTotalText,
                        storageText = recommended.storageText,
                        transferText = recommended.transferText,
                        badgeLabel = stringResource(sharedR.string.subscription_quota_best_for_you),
                        usagePercentage = recommended.usagePercentage,
                        usageLevel = recommended.usageLevel,
                        usageText = recommended.usageText,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotaWarningBottomBar(
    recommended: RecommendedCardData?,
    isHighestPlan: Boolean,
    onUpgradeClick: (Subscription) -> Unit,
    onViewAllPlansClick: () -> Unit,
    onContactSupportClick: () -> Unit,
) {
    val subscriptionToBuy = recommended?.subscriptionToBuy
    val upgradeText = recommended?.planName?.let {
        stringResource(sharedR.string.subscription_quota_upgrade_button, it)
    }
    val viewAllText = stringResource(sharedR.string.subscription_quota_view_all_plans)
    val contactSupportText = stringResource(sharedR.string.subscription_quota_contact_support)
    val buttonGroup = buildList<@Composable ColumnScope.() -> Button> {
        if (isHighestPlan) {
            add {
                Button.TextOnlyButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_QUOTA_WARNING_CONTACT_SUPPORT),
                    text = contactSupportText,
                    onClick = onContactSupportClick,
                )
            }
            return@buildList
        }
        if (subscriptionToBuy != null && upgradeText != null) {
            add {
                Button.PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_QUOTA_WARNING_UPGRADE_BUTTON),
                    text = upgradeText,
                    onClick = { onUpgradeClick(subscriptionToBuy) },
                )
            }
        }
        add {
            Button.TextOnlyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS),
                text = viewAllText,
                onClick = onViewAllPlansClick,
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        AnchoredButtonGroup(
            modifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH.dp)
                .align(Alignment.Center),
            buttonGroup = buttonGroup,
        )
    }
}

private fun currentUsagePercentage(state: QuotaWarningUpgradeState, metric: QuotaMetric): Float =
    when (metric) {
        QuotaMetric.Storage -> state.storageUsedPercentage.toFloat()
        QuotaMetric.Transfer -> state.transferUsedPercentage.toFloat()
    }

@Composable
private fun currentUsageText(
    state: QuotaWarningUpgradeState,
    metric: QuotaMetric,
    isProUser: Boolean,
    context: android.content.Context,
): String = when (metric) {
    QuotaMetric.Storage -> stringResource(
        sharedR.string.subscription_quota_current_plan_storage_usage,
        state.storageUsed?.let { formatFileSize(it, context) }.orEmpty(),
        state.storageTotal?.let { formatFileSize(it, context) }.orEmpty(),
    )

    QuotaMetric.Transfer -> if (isProUser) {
        stringResource(
            sharedR.string.subscription_quota_current_plan_transfer_usage_with_total,
            state.transferUsed?.let { formatFileSize(it, context) }.orEmpty(),
            state.transferTotal?.let { formatFileSize(it, context) }.orEmpty(),
        )
    } else {
        stringResource(
            sharedR.string.subscription_quota_current_plan_transfer_usage,
            state.transferUsed?.let { formatFileSize(it, context) }.orEmpty(),
        )
    }
}

@Composable
private fun recommendedCardData(
    subscription: LocalisedSubscription,
    state: QuotaWarningUpgradeState,
    metric: QuotaMetric,
    locale: Locale,
    context: android.content.Context,
): RecommendedCardData {
    // Match the user's current billing cycle: monthly subscribers see monthly (falling back to
    // yearly if no monthly option exists), everyone else (free/yearly/one-off) sees yearly.
    val useYearly = if (state.subscriptionCycle == AccountSubscriptionCycle.MONTHLY) {
        !subscription.hasSubscriptionFor(isMonthly = true)
    } else {
        subscription.hasSubscriptionFor(isMonthly = false)
    }
    val perMonthPrice = if (useYearly) {
        subscription.localisePriceOfYearlyAmountPerMonth(locale)?.price
            ?: subscription.localisePriceCurrencyCode(locale, isMonthly = false).price
    } else {
        subscription.localisePriceCurrencyCode(locale, isMonthly = true).price
    }
    val monthlyPriceText =
        stringResource(sharedR.string.choose_account_screen_only_price_per_month, perMonthPrice)
    val yearlyTotalText = if (useYearly) {
        stringResource(
            sharedR.string.subscription_quota_charged_yearly,
            subscription.localisePriceCurrencyCode(locale, isMonthly = false).price,
        )
    } else {
        null
    }

    val storageFormatted = subscription.formatStorageSize()
    val storageQuota = stringResource(storageFormatted.unit, storageFormatted.size)
    val storageText =
        stringResource(sharedR.string.choose_account_screen_storage_label, storageQuota)

    val transferFormatted = subscription.formatTransferSize(isMonthly = !useYearly)
    val transferQuota = stringResource(transferFormatted.unit, transferFormatted.size)
    val transferText =
        stringResource(sharedR.string.choose_account_screen_transfer_quota_label, transferQuota)

    val usedBytes = if (metric == QuotaMetric.Storage) state.storageUsed else state.transferUsed
    val quotaGb = if (metric == QuotaMetric.Storage) {
        subscription.storage
    } else {
        subscription.getSubscription(isMonthly = !useYearly)?.transfer
    }
    val usagePercentage = usagePercentageAgainstQuota(usedBytes, quotaGb)
    val usageText = when (metric) {
        QuotaMetric.Storage -> stringResource(
            sharedR.string.subscription_quota_recommended_storage_usage,
            usedBytes?.let { formatFileSize(it, context) }.orEmpty(),
            storageQuota,
        )

        QuotaMetric.Transfer -> stringResource(
            sharedR.string.subscription_quota_recommended_transfer_usage,
            usedBytes?.let { formatFileSize(it, context) }.orEmpty(),
            transferQuota,
        )
    }

    val offer = offerCardData(subscription, useYearly, locale)

    return RecommendedCardData(
        planName = stringResource(subscription.accountType.toUIAccountType().textValue),
        monthlyPriceText = monthlyPriceText,
        yearlyTotalText = yearlyTotalText,
        storageText = storageText,
        transferText = transferText,
        usagePercentage = usagePercentage,
        usageLevel = QuotaUsageLevel.Normal,
        usageText = usageText,
        subscriptionToBuy = subscription.getSubscription(isMonthly = !useYearly)
            ?: subscription.getSubscription(isMonthly = useYearly),
        offer = offer,
    )
}

/**
 * Builds the discount data for the recommended plan when its subscription (for the shown billing
 * cycle) carries an active offer, or null otherwise. Prices follow the same monthly/yearly wording
 * as the redesigned subscription page's offer cards.
 */
@Composable
private fun offerCardData(
    subscription: LocalisedSubscription,
    useYearly: Boolean,
    locale: Locale,
): RecommendedOfferData? {
    val isMonthly = !useYearly
    val offerSubscription = subscription.getSubscription(isMonthly)
    if (offerSubscription?.discountedAmountMonthly == null) return null

    val discountedMonthly =
        subscription.localiseDiscountedPriceMonthlyCurrencyCode(locale, isMonthly)?.price.orEmpty()
    val discountedYearly =
        subscription.localiseDiscountedPriceYearlyCurrencyCode(locale, isMonthly)?.price.orEmpty()
    val originalPrice = subscription.localisePriceCurrencyCode(locale, isMonthly).price

    val priceText: String
    val monthlyPriceText: String?
    val billedDiscountedPrice: String
    val billedOriginalPrice: String
    if (isMonthly) {
        priceText =
            stringResource(sharedR.string.subscription_revamp_price_per_month, discountedMonthly)
        monthlyPriceText = null
        billedDiscountedPrice = priceText
        billedOriginalPrice =
            stringResource(sharedR.string.subscription_revamp_price_per_month, originalPrice)
    } else {
        priceText =
            stringResource(sharedR.string.subscription_revamp_price_per_year, discountedYearly)
        monthlyPriceText =
            stringResource(sharedR.string.subscription_revamp_price_per_month, discountedMonthly)
        billedDiscountedPrice = discountedYearly
        billedOriginalPrice = originalPrice
    }

    return RecommendedOfferData(
        priceText = priceText,
        originalPriceText = originalPrice,
        discountDescriptionText = billedDescription(
            offerPeriod = offerSubscription.offerPeriod,
            isMonthly = isMonthly,
            discountedPrice = billedDiscountedPrice,
            originalPrice = billedOriginalPrice,
        ),
        discountBadgeText = getCampaignName(
            context = LocalContext.current,
            discountName = offerSubscription.discountName,
            discountPercentage = offerSubscription.discountedPercentage ?: 0,
        ),
        monthlyPriceText = monthlyPriceText,
    )
}

private const val CONTENT_MAX_WIDTH = 500

private const val MANAGE_PLAN_LINK_ANNOTATION = "mega.io"

/**
 * Test tag for the quota-warning screen close button
 */
const val TEST_TAG_QUOTA_WARNING_CLOSE = "quota_warning:close"

/**
 * Test tag for the quota-warning screen illustration
 */
const val TEST_TAG_QUOTA_WARNING_ILLUSTRATION = "quota_warning:illustration"

/**
 * Test tag for the quota-warning screen title
 */
const val TEST_TAG_QUOTA_WARNING_TITLE = "quota_warning:title"

/**
 * Test tag for the quota-warning screen subtitle
 */
const val TEST_TAG_QUOTA_WARNING_SUBTITLE = "quota_warning:subtitle"

/**
 * Test tag for the quota-warning screen "Learn more" link
 */
const val TEST_TAG_QUOTA_WARNING_LEARN_MORE = "quota_warning:learn_more"

/**
 * Test tag for the quota-warning screen upgrade button
 */
const val TEST_TAG_QUOTA_WARNING_UPGRADE_BUTTON = "quota_warning:upgrade_button"

/**
 * Test tag for the quota-warning screen "View all plans" link
 */
const val TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS = "quota_warning:view_all_plans"

/**
 * Test tag for the quota-warning screen "Contact support" button (highest-plan scenario)
 */
const val TEST_TAG_QUOTA_WARNING_CONTACT_SUPPORT = "quota_warning:contact_support"

/**
 * Test tag for the quota-warning screen skeleton
 */
const val TEST_TAG_QUOTA_WARNING_SKELETON = "quota_warning:skeleton"

@CombinedThemePreviews
@Composable
private fun QuotaWarningUpgradeContentPreview(
    @PreviewParameter(QuotaWarningPreviewProvider::class) preview: QuotaWarningPreviewState,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaWarningUpgradeContent(
            illustrationRes = IconPackR.drawable.illustration_mega_secondary_quota_warning,
            title = preview.title,
            subtitle = preview.subtitle,
            showLearnMore = preview.showLearnMore,
            subtitleHasLink = preview.subtitleHasLink,
            isHighestPlan = preview.isHighestPlan,
            isLoading = preview.isLoading,
            currentCard = preview.currentCard,
            recommended = preview.recommended,
            onUpgradeClick = {},
            onViewAllPlansClick = {},
            onLearnMoreClick = {},
            onContactSupportClick = {},
            onManagePlanClick = {},
            onClose = {},
        )
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 740, heightDp = 360)
@Composable
private fun QuotaWarningUpgradeContentLandscapePreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaWarningUpgradeContent(
            illustrationRes = IconPackR.drawable.illustration_mega_secondary_quota_warning,
            title = "Your storage is 80% full",
            subtitle = "Upgrade your plan before you run out of space",
            showLearnMore = false,
            subtitleHasLink = false,
            isHighestPlan = false,
            isLoading = false,
            currentCard = CurrentCardData(
                planName = "Free",
                currentPlanLabel = "Current plan",
                usagePercentage = 95f,
                usageLevel = QuotaUsageLevel.Warning,
                usageText = "19 GB of 20 GB used",
            ),
            recommended = RecommendedCardData(
                planName = "Essential",
                monthlyPriceText = "€3.33/month",
                yearlyTotalText = "€40.01 charged yearly",
                storageText = "200 GB storage",
                transferText = "2.4 TB transfer",
                usagePercentage = 10f,
                usageLevel = QuotaUsageLevel.Normal,
                usageText = "19 GB of 200 GB used",
                subscriptionToBuy = null,
            ),
            onUpgradeClick = {},
            onViewAllPlansClick = {},
            onLearnMoreClick = {},
            onContactSupportClick = {},
            onManagePlanClick = {},
            onClose = {},
        )
    }
}

private data class QuotaWarningPreviewState(
    val title: String,
    val subtitle: String,
    val showLearnMore: Boolean,
    val isLoading: Boolean,
    val currentCard: CurrentCardData,
    val recommended: RecommendedCardData?,
    val subtitleHasLink: Boolean = false,
    val isHighestPlan: Boolean = false,
)

private class QuotaWarningPreviewProvider : PreviewParameterProvider<QuotaWarningPreviewState> {
    private val storageCurrentAlmostFull = CurrentCardData(
        planName = "Free",
        currentPlanLabel = "Current plan",
        usagePercentage = 80f,
        usageLevel = QuotaUsageLevel.Warning,
        usageText = "19 GB of 20 GB used",
    )
    private val storageCurrentFull = storageCurrentAlmostFull.copy(
        usagePercentage = 100f,
        usageLevel = QuotaUsageLevel.Error,
        usageText = "20 GB of 20 GB used",
    )
    private val storageRecommended = RecommendedCardData(
        planName = "Essential",
        monthlyPriceText = "€3.33/month",
        yearlyTotalText = "€40.01 charged yearly",
        storageText = "200 GB storage",
        transferText = "2.4 TB transfer",
        usagePercentage = 10f,
        usageLevel = QuotaUsageLevel.Normal,
        usageText = "19 GB of 200 GB used",
        subscriptionToBuy = null,
    )

    private val storageRecommendedOffer = storageRecommended.copy(
        planName = "Pro I",
        monthlyPriceText = "€4.99/month",
        yearlyTotalText = "€29.94 charged yearly",
        storageText = "2 TB cloud storage",
        transferText = "2 TB transfer",
        usageText = "Storage: 19 GB out of 2 TB",
        offer = RecommendedOfferData(
            priceText = "€29.94 charged yearly",
            originalPriceText = "€59.88",
            discountDescriptionText = "Billed at €29.94 for the first year, €119.88 charged yearly after",
            discountBadgeText = "Special offer · 50% off",
            monthlyPriceText = "€4.99/month",
        ),
    )

    private val transferCurrentFree = CurrentCardData(
        planName = "Free",
        currentPlanLabel = "Current plan",
        usagePercentage = 90f,
        usageLevel = QuotaUsageLevel.Warning,
        usageText = "1 GB used",
    )
    private val transferCurrentPro = transferCurrentFree.copy(
        planName = "Essential",
        usagePercentage = 95f,
        usageText = "2.3 TB of 2.4 TB used",
    )
    private val transferCurrentExceeded = transferCurrentFree.copy(
        usagePercentage = 100f,
        usageLevel = QuotaUsageLevel.Error,
    )
    private val transferRecommended =
        storageRecommended.copy(usageText = "1 GB of 2.4 TB used")

    private val transferCurrentProIIILow = CurrentCardData(
        planName = "Pro III",
        currentPlanLabel = "Current plan",
        usagePercentage = 97f,
        usageLevel = QuotaUsageLevel.Warning,
        usageText = "233 TB of 240 TB used",
    )
    private val transferCurrentProIIIExceeded = transferCurrentProIIILow.copy(
        usagePercentage = 100f,
        usageLevel = QuotaUsageLevel.Error,
        usageText = "240 TB of 240 TB used",
    )

    override val values = sequenceOf(
        QuotaWarningPreviewState(
            title = "Your storage is 80% full",
            subtitle = "Upgrade your plan before you run out of space",
            showLearnMore = false,
            isLoading = false,
            currentCard = storageCurrentAlmostFull,
            recommended = storageRecommended,
        ),
        QuotaWarningPreviewState(
            title = "Your storage is 100% full",
            subtitle = "Upgrade your plan to get more storage and upload more files",
            showLearnMore = false,
            isLoading = false,
            currentCard = storageCurrentFull,
            recommended = storageRecommended,
        ),
        QuotaWarningPreviewState(
            title = "Your storage is 100% full",
            subtitle = "You've run out of storage space. Upgrade your plan to continue uploading",
            showLearnMore = false,
            isLoading = false,
            currentCard = storageCurrentFull,
            recommended = storageRecommended,
        ),
        QuotaWarningPreviewState(
            title = "Your storage is 80% full",
            subtitle = "Upgrade your plan before you run out of space",
            showLearnMore = false,
            isLoading = false,
            currentCard = storageCurrentAlmostFull,
            recommended = storageRecommendedOffer,
        ),
        QuotaWarningPreviewState(
            title = "Your transfer quota is running low",
            subtitle = "As a result, your download may be interrupted. Upgrade your plan to get more transfer quota.",
            showLearnMore = true,
            isLoading = false,
            currentCard = transferCurrentFree,
            recommended = transferRecommended,
        ),
        QuotaWarningPreviewState(
            title = "You've used 95% of your transfer quota",
            subtitle = "As a result, media playback may be interrupted. Upgrade your plan to get more transfer quota.",
            showLearnMore = true,
            isLoading = false,
            currentCard = transferCurrentPro,
            recommended = transferRecommended,
        ),
        QuotaWarningPreviewState(
            title = "Transfer quota exceeded",
            subtitle = "To continue your download, upgrade your plan to get more transfer quota.",
            showLearnMore = true,
            isLoading = false,
            currentCard = transferCurrentExceeded,
            recommended = transferRecommended,
        ),
        QuotaWarningPreviewState(
            title = "Your storage is 100% full",
            subtitle = "Make room in Cloud drive, or manage your plan at [A]mega.io[/A] for more storage",
            showLearnMore = false,
            subtitleHasLink = true,
            isHighestPlan = true,
            isLoading = false,
            currentCard = storageCurrentFull.copy(
                planName = "Pro III",
                usageText = "10 TB of 10 TB used",
            ),
            recommended = null,
        ),
        QuotaWarningPreviewState(
            title = "You've used 97% of your transfer quota",
            subtitle = "As a result, your download may be interrupted. Manage your plan at [A]mega.io[/A] for more transfer quota",
            showLearnMore = false,
            subtitleHasLink = true,
            isHighestPlan = true,
            isLoading = false,
            currentCard = transferCurrentProIIILow,
            recommended = null,
        ),
        QuotaWarningPreviewState(
            title = "You've used 97% of your transfer quota",
            subtitle = "As a result, media playback may be interrupted. Manage your plan at [A]mega.io[/A] for more transfer quota",
            showLearnMore = false,
            subtitleHasLink = true,
            isHighestPlan = true,
            isLoading = false,
            currentCard = transferCurrentProIIILow,
            recommended = null,
        ),
        QuotaWarningPreviewState(
            title = "Transfer quota exceeded",
            subtitle = "To continue your download, manage your plan at [A]mega.io[/A] for more transfer quota",
            showLearnMore = false,
            subtitleHasLink = true,
            isHighestPlan = true,
            isLoading = false,
            currentCard = transferCurrentProIIIExceeded,
            recommended = null,
        ),
        QuotaWarningPreviewState(
            title = "Transfer quota exceeded",
            subtitle = "To continue media playback, manage your plan at [A]mega.io[/A] for more transfer quota",
            showLearnMore = false,
            subtitleHasLink = true,
            isHighestPlan = true,
            isLoading = false,
            currentCard = transferCurrentProIIIExceeded,
            recommended = null,
        ),
        QuotaWarningPreviewState(
            title = "",
            subtitle = "",
            showLearnMore = false,
            isLoading = true,
            currentCard = storageCurrentAlmostFull,
            recommended = null,
        ),
    )
}
