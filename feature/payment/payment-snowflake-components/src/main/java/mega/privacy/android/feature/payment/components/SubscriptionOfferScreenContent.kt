package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.button.SecondaryNavigationIconButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Content of the full-screen subscription offer landing screen (DSN-3130): the campaign artwork
 * drawn edge-to-edge behind the status bar and fading into the page background, a dismiss (X)
 * affordance, a promotional header (badge, title, campaign name, countdown), the discounted plan
 * as an [OfferPriceCard] (without its own buy button) and a pinned [BuyPlanBottomBar] CTA.
 *
 * The countdown is shown whenever the offer carries an expiry, including after it has passed, where
 * it settles on all zeros so the user watching the deal run out sees it end.
 *
 * @param campaignText the campaign name (e.g. "Black Friday: 50% off"), shown as the header
 * subtitle and as the plan card badge
 * @param validUntil the offer expiry as epoch seconds, null to hide the countdown
 * @param validUntilText the countdown caption (e.g. "valid until July 11, 2026"), null to hide
 * the countdown
 * @param planName the plan name (e.g. "Pro I")
 * @param priceText the discounted price (e.g. "€4.99/month")
 * @param originalPriceText the pre-discount price shown with a strikethrough (e.g. "€9.99")
 * @param discountDescriptionText the discount description (e.g. "Billed at €4.99/month for the
 * first 12 months, €9.99/month after")
 * @param storageText storage benefit (e.g. "2 TB cloud storage")
 * @param transferText transfer benefit (e.g. "2 TB transfer")
 * @param buyButtonText the pinned CTA label (e.g. "Get Pro I")
 * @param onBuyClick called when the pinned CTA is tapped
 * @param onDismissClick called when the dismiss (X) icon is tapped
 * @param modifier
 * @param monthlyPriceText the per-month price shown above the yearly total (e.g. "€4.99/month"),
 * null for monthly plans
 * @param useBrandButton whether the CTA uses the brand (campaign) button; false renders the plain
 * button the deal-ended state uses
 * @param viewAllPlansText label of the text button below the CTA that opens the full list of plans,
 * null when only the promoted plan carries the campaign
 * @param onViewAllPlansClick called when the view-all-plans text button is tapped
 */
@Composable
fun SubscriptionOfferScreenContent(
    campaignText: String,
    validUntil: Long?,
    validUntilText: String?,
    planName: String,
    priceText: String,
    originalPriceText: String,
    discountDescriptionText: String,
    storageText: String,
    transferText: String,
    buyButtonText: String,
    onBuyClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    monthlyPriceText: String? = null,
    useBrandButton: Boolean = true,
    viewAllPlansText: String? = null,
    onViewAllPlansClick: () -> Unit = {},
) {
    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BuyPlanBottomBar(
                text = buyButtonText,
                onClick = onBuyClick,
                textOnlyButtonText = viewAllPlansText,
                onTextOnlyButtonClick = onViewAllPlansClick,
                maxContentWidth = SUBSCRIPTION_OFFER_CONTENT_MAX_WIDTH,
                useBrandButton = useBrandButton,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DSTokens.colors.background.pageBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(iconPackR.drawable.subscription_offer_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BANNER_HEIGHT)
                        .testTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_BANNER),
                )
                HeaderImageFade(
                    modifier = Modifier.padding(top = BANNER_HEIGHT - HEADER_IMAGE_FADE_HEIGHT),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = HEADER_TOP)
                        .widthIn(max = SUBSCRIPTION_OFFER_CONTENT_MAX_WIDTH)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Badge(
                        badgeType = BadgeType.Mega,
                        text = stringResource(sharedR.string.subscription_offer_special_offer_badge),
                        modifier = Modifier.testTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_BADGE),
                    )
                    MegaText(
                        text = stringResource(sharedR.string.subscription_revamp_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_TITLE),
                    )
                    MegaText(
                        text = campaignText,
                        style = MaterialTheme.typography.headlineSmall,
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_CAMPAIGN),
                    )
                    if (validUntil != null && validUntilText != null) {
                        OfferCountdownSection(
                            validUntil = validUntil,
                            validUntilText = validUntilText,
                        )
                    }
                }
                SecondaryNavigationIconButton(
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    onClick = onDismissClick,
                    contentDescription = stringResource(sharedR.string.general_dismiss_dialog),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .testTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_DISMISS),
                )
            }
            OfferPriceCard(
                planName = planName,
                priceText = priceText,
                originalPriceText = originalPriceText,
                discountDescriptionText = discountDescriptionText,
                discountBadgeText = campaignText,
                storageText = storageText,
                transferText = transferText,
                monthlyPriceText = monthlyPriceText,
                modifier = Modifier
                    .widthIn(max = SUBSCRIPTION_OFFER_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

/**
 * Renders the offer countdown driven by [validUntil] (epoch seconds).
 *
 * Unlike the banners, this stays on screen at "00 / 00 / 00" once elapsed (DSN-3130), and shares
 * [rememberOfferRemaining] with the buy CTA so the two agree on when the deal is over.
 */
@Composable
private fun OfferCountdownSection(
    validUntil: Long,
    validUntilText: String,
    modifier: Modifier = Modifier,
) {
    val units = offerCountdownUnits(rememberOfferRemaining(validUntil))
    OfferCountdown(
        validUntilText = validUntilText,
        days = units.daysText,
        hours = units.hoursText,
        minutes = units.minutesText,
        daysLabel = pluralStringResource(
            sharedR.plurals.subscription_offer_countdown_days,
            units.days.toInt(),
        ),
        hoursLabel = pluralStringResource(
            sharedR.plurals.subscription_offer_countdown_hours,
            units.hours.toInt(),
        ),
        minutesLabel = pluralStringResource(
            sharedR.plurals.subscription_offer_countdown_minutes,
            units.minutes.toInt(),
        ),
        modifier = modifier,
    )
}

private val BANNER_HEIGHT = 220.dp

/**
 * Header content overlaps the bottom of the banner artwork, replicating the Figma layout where the
 * title sits on the gradient fade.
 */
private val HEADER_TOP = 160.dp

/**
 * On wide screens the offer content stays this narrow and centred, matching the landscape layout in
 * the design, so the header, plan card and CTA keep readable line lengths. Only the banner artwork
 * spans the full width.
 */
internal val SUBSCRIPTION_OFFER_CONTENT_MAX_WIDTH = 500.dp

@CombinedThemePreviews
@Composable
private fun SubscriptionOfferScreenContentPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        SubscriptionOfferScreenContent(
            campaignText = "Black Friday: 50% off",
            validUntil = System.currentTimeMillis() / 1000L +
                    28L * 24L * 3600L + 12L * 3600L + 90L,
            validUntilText = "valid until July 11, 2026",
            planName = "Pro I",
            priceText = "€4.99/month",
            originalPriceText = "€9.99",
            discountDescriptionText = "Billed at €4.99/month for the first 12 months, €9.99/month after",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I",
            onBuyClick = {},
            onDismissClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SubscriptionOfferScreenContentMultipleOffersPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        SubscriptionOfferScreenContent(
            campaignText = "Black Friday: 50% off",
            validUntil = System.currentTimeMillis() / 1000L +
                    28L * 24L * 3600L + 12L * 3600L + 90L,
            validUntilText = "valid until July 11, 2026",
            planName = "Pro I",
            priceText = "€4.99/month",
            originalPriceText = "€9.99",
            discountDescriptionText = "Billed at €4.99/month for the first 12 months, €9.99/month after",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I",
            onBuyClick = {},
            onDismissClick = {},
            viewAllPlansText = "View all plans",
        )
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 917, heightDp = 500)
@Composable
private fun SubscriptionOfferScreenContentLandscapePreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        SubscriptionOfferScreenContent(
            campaignText = "Black Friday: 50% off",
            validUntil = System.currentTimeMillis() / 1000L +
                    28L * 24L * 3600L + 12L * 3600L + 90L,
            validUntilText = "valid until July 11, 2026",
            planName = "Pro I",
            priceText = "€4.99/month",
            originalPriceText = "€9.99",
            discountDescriptionText = "Billed at €4.99/month for the first 12 months, €9.99/month after",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I",
            onBuyClick = {},
            onDismissClick = {},
        )
    }
}

/**
 * Tag for the SubscriptionOfferScreenContent root container
 */
const val TEST_TAG_SUBSCRIPTION_OFFER_SCREEN = "subscription_offer_screen"

/**
 * Tag for the SubscriptionOfferScreenContent banner artwork
 */
const val TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_BANNER = "subscription_offer_screen:banner"

/**
 * Tag for the SubscriptionOfferScreenContent dismiss icon
 */
const val TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_DISMISS = "subscription_offer_screen:dismiss"

/**
 * Tag for the SubscriptionOfferScreenContent header badge
 */
const val TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_BADGE = "subscription_offer_screen:badge"

/**
 * Tag for the SubscriptionOfferScreenContent header title
 */
const val TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_TITLE = "subscription_offer_screen:title"

/**
 * Tag for the SubscriptionOfferScreenContent header campaign name
 */
const val TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_CAMPAIGN = "subscription_offer_screen:campaign"
