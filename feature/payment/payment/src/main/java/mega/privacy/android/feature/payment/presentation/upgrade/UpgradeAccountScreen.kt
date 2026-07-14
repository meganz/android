package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.components.AdditionalBenefitProPlanView
import mega.privacy.android.feature.payment.components.BuyPlanBottomBar
import mega.privacy.android.feature.payment.components.FreePlanCard
import mega.privacy.android.feature.payment.components.NewFeatureRow
import mega.privacy.android.feature.payment.components.TEST_TAG_FREE_PLAN_CARD
import mega.privacy.android.feature.payment.components.UpgradeAccountScreenTopBar
import mega.privacy.android.feature.payment.components.upgradeAccountRevampSkeleton
import mega.privacy.android.feature.payment.model.AccountStorageUIState
import mega.privacy.android.feature.payment.model.OfferHighlight
import mega.privacy.android.feature.payment.model.ProFeature
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeAccountScreen(
    onInAppCheckoutClick: (Subscription) -> Unit,
    maybeLaterClicked: () -> Unit,
    onFreePlanClicked: () -> Unit,
    onBack: () -> Unit,
    uiState: UpgradeAccountState = UpgradeAccountState(),
    accountStorageUiState: AccountStorageUIState = AccountStorageUIState(),
    isNewCreationAccount: Boolean = false,
    isUpgradeAccount: Boolean = false,
    isSubscriptionRevampEnabled: Boolean = false,
    onSubscriptionUnavailableLearnMoreClick: () -> Unit = {},
    onPricingPageClick: () -> Unit = {},
) {
    var chosenPlan by rememberSaveable { mutableStateOf<AccountType?>(null) }
    var isMonthly by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = LocalLocale.current.platformLocale
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLandscapeRevamp = isLandscape && isSubscriptionRevampEnabled

    val lazyListState = rememberLazyListState()
    val topBarHeightPx =
        with(LocalDensity.current) { 56.dp.roundToPx() + WindowInsets.statusBars.getTop(this) }
    val headerHeightPx = with(LocalDensity.current) { HEADER_IMAGE_HEIGHT.roundToPx() }
    val position by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val itemOffset by remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }
    val currentHeaderHeightPx = headerHeightPx - itemOffset
    val showFullSkeleton = isSubscriptionRevampEnabled &&
            uiState.localisedSubscriptionsList.isEmpty() &&
            uiState.isSubscriptionFeatureAvailable != false
    val transparent = isLandscapeRevamp ||
            (!showFullSkeleton && position == 0 && currentHeaderHeightPx > topBarHeightPx)
    val alpha by animateFloatAsState(targetValue = if (transparent) 0f else 1f)
    val snackBarHostState = remember { SnackbarHostState() }

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

    val offerHighlight = if (isSubscriptionRevampEnabled) {
        uiState.offerHighlight(isMonthly, isUpgradeAccount)
    } else {
        OfferHighlight.None
    }
    val showOfferBanner = offerHighlight is OfferHighlight.Single

    // pre select discounted plan if user has discount and no plan is currently selected
    LaunchedEffect(uiState.localisedSubscriptionsList) {
        if (chosenPlan == null) {
            uiState.localisedSubscriptionsList.find { it.hasDiscount }?.let {
                chosenPlan = it.accountType
            }
        }
    }

    // if the user changes billing period, check if the currently selected plan is available for that period. If not, unselect the plan and show a message.
    LaunchedEffect(uiState.localisedSubscriptionsList, isMonthly) {
        val plan = chosenPlan
        if (plan != null) {
            val selectedSubscription = uiState.localisedSubscriptionsList.find {
                it.accountType == plan
            }
            val isPlanAvailable = selectedSubscription?.hasSubscriptionFor(isMonthly) == true
            if (!isPlanAvailable) {
                val planName = resources.getString(plan.toUIAccountType().textValue)
                val message = if (selectedSubscription?.hasSubscriptionFor(true) == true) {
                    resources.getString(
                        sharedR.string.choose_account_screen_plan_available_monthly_billing,
                        planName
                    )
                } else {
                    resources.getString(
                        sharedR.string.choose_account_screen_plan_available_yearly_billing,
                        planName
                    )
                }
                chosenPlan = null
                snackBarHostState.showAutoDurationSnackbar(message)
            }
        }
    }

    val proFeatures = remember(highestStorageString) {
        listOf(
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.Cloud,
                title = resources.getString(sharedR.string.pro_plan_feature_storage_title),
                description = resources.getString(
                    sharedR.string.pro_plan_feature_storage_desc,
                    highestStorageString
                ),
                testTag = "pro_plan:feature:storage"
            ),
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
                title = resources.getString(sharedR.string.pro_plan_feature_transfer_title),
                description = resources.getString(sharedR.string.pro_plan_feature_transfer_desc),
                testTag = "pro_plan:feature:transfer"
            ),
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.VPN,
                title = resources.getString(sharedR.string.pro_plan_feature_vpn_title),
                description = resources.getString(sharedR.string.pro_plan_feature_vpn_desc),
                testTag = "pro_plan:feature:vpn"
            ),
            ProFeature(
                icon = IconPack.Medium.Thin.Outline.LockKeyholeCircle,
                title = resources.getString(sharedR.string.pro_plan_feature_pass_title),
                description = resources.getString(sharedR.string.pro_plan_feature_pass_desc),
                testTag = "pro_plan:feature:pass"
            )
        )
    }

    MegaScaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            UpgradeAccountScreenTopBar(
                alpha = alpha,
                isUpgradeAccount = isUpgradeAccount,
                maybeLaterClicked = maybeLaterClicked,
                onBack = onBack
            )
        },
        snackbarHost = {
            MegaSnackbar(snackBarHostState = snackBarHostState)
        },
        bottomBar = {
            if (!isSubscriptionRevampEnabled && uiState.isSubscriptionFeatureAvailable == true) {
                chosenPlan?.takeIf {
                    !isCurrentPlan(
                        uiState = uiState,
                        subscriptionAccountType = it,
                        isMonthly = isMonthly,
                        isUpgradeAccount = isUpgradeAccount
                    )
                }?.let { accountType ->
                    val selectedSubscription = uiState.localisedSubscriptionsList
                        .find { sub -> sub.accountType == chosenPlan }
                    val subscriptionForPeriod = selectedSubscription?.getSubscription(isMonthly)

                    if (subscriptionForPeriod != null) {
                        BuyPlanBottomBar(
                            modifier = Modifier,
                            text = stringResource(
                                accountType.toUIAccountType().textBuyButtonValue
                            ),
                            onClick = {
                                selectedSubscription?.getSubscription(isMonthly)?.let {
                                    onInAppCheckoutClick(it)
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val bodyContent: LazyListScope.() -> Unit = {
            if (showFullSkeleton) {
                upgradeAccountRevampSkeleton()
            } else {
                if (isSubscriptionRevampEnabled) {
                    when (offerHighlight) {
                        is OfferHighlight.Single -> subscriptionOfferContent(
                            uiState = uiState,
                            offerSubscription = offerHighlight.subscription,
                            isMonthly = isMonthly,
                            onMonthlyChange = { isMonthly = it },
                            locale = locale,
                            context = context,
                            isUpgradeAccount = isUpgradeAccount,
                            onInAppCheckoutClick = onInAppCheckoutClick,
                            onSubscriptionUnavailableLearnMoreClick = onSubscriptionUnavailableLearnMoreClick,
                            onPricingPageClick = onPricingPageClick,
                        )

                        // TODO: render a dedicated multiple-offer layout for OfferHighlight.Multiple.
                        //  Until then multiple concurrent offers use the standard revamp layout.
                        is OfferHighlight.Multiple,
                        OfferHighlight.None,
                            -> subscriptionRevampContent(
                            uiState = uiState,
                            isMonthly = isMonthly,
                            onMonthlyChange = { isMonthly = it },
                            locale = locale,
                            isUpgradeAccount = isUpgradeAccount,
                            onInAppCheckoutClick = onInAppCheckoutClick,
                            onSubscriptionUnavailableLearnMoreClick = onSubscriptionUnavailableLearnMoreClick,
                            onPricingPageClick = onPricingPageClick,
                        )
                    }
                } else {
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

                    if (uiState.isSubscriptionFeatureAvailable == false) {
                        subscriptionUnavailableContent(onLearnMoreClick = onSubscriptionUnavailableLearnMoreClick)
                    } else {
                        subscriptionAvailableContent(
                            uiState = uiState,
                            isMonthly = isMonthly,
                            onMonthlyChange = { isMonthly = it },
                            chosenPlan = chosenPlan,
                            onPlanSelected = { chosenPlan = it },
                            hasDiscount = hasDiscount,
                            context = context,
                            locale = locale,
                            isUpgradeAccount = isUpgradeAccount,
                        )
                    }
                }

                item("additional_benefits") {
                    AdditionalBenefitProPlanView(
                        title = stringResource(id = sharedR.string.pro_plan_additional_benefits_section_title),
                        benefits = listOf(
                            stringResource(id = sharedR.string.pro_plan_benefit_password_protected_links),
                            stringResource(id = sharedR.string.pro_plan_benefit_links_with_expiry_dates),
                            stringResource(id = sharedR.string.pro_plan_benefit_auto_sync_mobile),
                            stringResource(
                                id = sharedR.string.pro_plan_benefit_rewind_days,
                                if (isSubscriptionRevampEnabled) 180 else 60
                            ),
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

        if (isLandscapeRevamp) {
            LandscapeUpgradeAccountLayout(
                showOfferBanner = showOfferBanner,
                lazyListState = lazyListState,
                innerPadding = innerPadding,
                content = bodyContent,
            )
        } else {
            PortraitUpgradeAccountLayout(
                showFullSkeleton = showFullSkeleton,
                showOfferBanner = showOfferBanner,
                lazyListState = lazyListState,
                innerPadding = innerPadding,
                content = bodyContent,
            )
        }
    }
}

/**
 * Header artwork for the upgrade screen: the seasonal offer banner when an offer is being
 * highlighted, otherwise the standard Pro header image. Rendered as a full-width top banner in
 * portrait and as the full-height left panel in the landscape two-pane layout.
 */
@Composable
private fun UpgradeAccountHeaderImage(
    showOfferBanner: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier.testTag(TEST_TAG_IMAGE_HEADER),
        painter = painterResource(
            if (showOfferBanner) {
                IconPackR.drawable.subscription_offer_banner
            } else {
                IconPackR.drawable.choose_account_type_header
            }
        ),
        contentDescription = "Header Image",
        contentScale = ContentScale.Crop,
    )
}

/**
 * Default single-column layout: the header image scrolls as the first item above [content]. The
 * image is omitted while the full-page skeleton is shown.
 */
@Composable
private fun PortraitUpgradeAccountLayout(
    showFullSkeleton: Boolean,
    showOfferBanner: Boolean,
    lazyListState: LazyListState,
    innerPadding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .testTag(TEST_TAG_LAZY_COLUMN)
            .padding(top = if (showFullSkeleton) innerPadding.calculateTopPadding() else 0.dp)
            .padding(bottom = innerPadding.calculateBottomPadding())
            .fillMaxSize(),
        state = lazyListState,
    ) {
        if (!showFullSkeleton) {
            item("image_header") {
                UpgradeAccountHeaderImage(
                    showOfferBanner = showOfferBanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_IMAGE_HEIGHT),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        content()
    }
}

/**
 * Landscape two-pane layout for the revamped subscription page: the header image fills the left
 * panel edge-to-edge while [content] scrolls in the right column (DSN-3131 landscape design). The
 * right column clears only the status bar; the transparent top app bar's back button floats over
 * the image on the left.
 */
@Composable
private fun LandscapeUpgradeAccountLayout(
    showOfferBanner: Boolean,
    lazyListState: LazyListState,
    innerPadding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        UpgradeAccountHeaderImage(
            showOfferBanner = showOfferBanner,
            modifier = Modifier
                .weight(LANDSCAPE_IMAGE_WEIGHT)
                .fillMaxHeight(),
        )
        LazyColumn(
            modifier = Modifier
                .testTag(TEST_TAG_LAZY_COLUMN)
                .weight(LANDSCAPE_CONTENT_WEIGHT)
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            state = lazyListState,
            content = content,
        )
    }
}

fun isCurrentPlan(
    uiState: UpgradeAccountState,
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
    is OfferPeriod.Month -> if (period.value == 1) {
        stringResource(
            id = sharedR.string.label_first_time_in_months_full_singular,
            discountedPrice
        )
    } else {
        pluralStringResource(
            id = sharedR.plurals.label_first_time_in_months_full,
            period.value,
            discountedPrice,
            period.value
        )
    }

    is OfferPeriod.Year -> if (period.value == 1) {
        stringResource(
            id = sharedR.string.label_first_time_in_years_full_singular,
            discountedPrice
        )
    } else {
        pluralStringResource(
            id = sharedR.plurals.label_first_time_in_years_full,
            period.value,
            discountedPrice,
            period.value
        )
    }
}

fun getCampaignName(context: Context, discountName: String?, discountPercentage: Int): String =
    if (discountName.isNullOrBlank()) {
        context.getString(sharedR.string.campaign_name_special_offer, discountPercentage)
    } else {
        context.getString(
            sharedR.string.campaign_name_with_discount,
            discountName,
            discountPercentage,
        )
    }

@CombinedThemePreviews
@Composable
internal fun ChooseAccountScreenPreview(
    @PreviewParameter(UpgradeAccountPreviewProvider::class) state: UpgradeAccountState,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        UpgradeAccountScreen(
            uiState = state,
            accountStorageUiState = AccountStorageUIState(
                baseStorage = 15L * 1024 * 1024 * 1024,
                totalStorage = 100L * 1024 * 1024 * 1024,
            ),
            isNewCreationAccount = false,
            isUpgradeAccount = false,
            onInAppCheckoutClick = { },
            onFreePlanClicked = {},
            maybeLaterClicked = {},
            onBack = {}
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun UpgradeAccountScreenPreview(
    @PreviewParameter(UpgradeAccountPreviewProvider::class) state: UpgradeAccountState,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        UpgradeAccountScreen(
            uiState = state,
            accountStorageUiState = AccountStorageUIState(
                baseStorage = 15L * 1024 * 1024 * 1024,
                totalStorage = 100L * 1024 * 1024 * 1024,
            ),
            isNewCreationAccount = false,
            isUpgradeAccount = true,
            onInAppCheckoutClick = { },
            onFreePlanClicked = {},
            maybeLaterClicked = {},
            onBack = {}
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun UpgradeAccountScreenRevampPreview(
    @PreviewParameter(UpgradeAccountPreviewProvider::class) state: UpgradeAccountState,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        UpgradeAccountScreen(
            uiState = state.copy(
                isSubscriptionFeatureAvailable = true,
                currentSubscriptionPlan = AccountType.PRO_I,
                subscriptionCycle = AccountSubscriptionCycle.YEARLY,
                subscriptionStatus = SubscriptionStatus.VALID,
                subscriptionRenewTime = 1_815_000_000L,
                cheapestSubscriptionAvailable = state.localisedSubscriptionsList.getOrNull(2),
            ),
            accountStorageUiState = AccountStorageUIState(
                baseStorage = 15L * 1024 * 1024 * 1024,
                totalStorage = 100L * 1024 * 1024 * 1024,
            ),
            isNewCreationAccount = false,
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            onInAppCheckoutClick = { },
            onFreePlanClicked = {},
            maybeLaterClicked = {},
            onBack = {}
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun UpgradeAccountScreenSingleOfferPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        UpgradeAccountScreen(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = UpgradeAccountPreviewProvider.singleOfferSubscriptionsList,
                isSubscriptionFeatureAvailable = true,
                cheapestSubscriptionAvailable = UpgradeAccountPreviewProvider.subscriptionProLite,
            ),
            accountStorageUiState = AccountStorageUIState(
                baseStorage = 15L * 1024 * 1024 * 1024,
                totalStorage = 100L * 1024 * 1024 * 1024,
            ),
            isNewCreationAccount = false,
            isUpgradeAccount = false,
            isSubscriptionRevampEnabled = true,
            onInAppCheckoutClick = { },
            onFreePlanClicked = {},
            maybeLaterClicked = {},
            onBack = {}
        )
    }
}

@Preview(name = "Revamp landscape", widthDp = 800, heightDp = 400)
@Composable
private fun UpgradeAccountScreenRevampLandscapePreview(
    @PreviewParameter(UpgradeAccountPreviewProvider::class) state: UpgradeAccountState,
) {
    val landscapeConfiguration = Configuration(LocalConfiguration.current).apply {
        orientation = Configuration.ORIENTATION_LANDSCAPE
    }
    AndroidTheme(isSystemInDarkTheme()) {
        CompositionLocalProvider(LocalConfiguration provides landscapeConfiguration) {
            UpgradeAccountScreen(
                uiState = state.copy(
                    isSubscriptionFeatureAvailable = true,
                    currentSubscriptionPlan = AccountType.PRO_I,
                    subscriptionCycle = AccountSubscriptionCycle.YEARLY,
                    subscriptionStatus = SubscriptionStatus.VALID,
                    subscriptionRenewTime = 1_815_000_000L,
                    cheapestSubscriptionAvailable = state.localisedSubscriptionsList.getOrNull(2),
                ),
                accountStorageUiState = AccountStorageUIState(
                    baseStorage = 15L * 1024 * 1024 * 1024,
                    totalStorage = 100L * 1024 * 1024 * 1024,
                ),
                isNewCreationAccount = false,
                isUpgradeAccount = true,
                isSubscriptionRevampEnabled = true,
                onInAppCheckoutClick = { },
                onFreePlanClicked = {},
                maybeLaterClicked = {},
                onBack = {}
            )
        }
    }
}

/**
 * Height of the header image when shown as the portrait top banner.
 */
private val HEADER_IMAGE_HEIGHT = 180.dp

/**
 * Left image panel width weight in the landscape revamp two-pane layout (matches the Figma
 * 407/1133 split).
 */
private const val LANDSCAPE_IMAGE_WEIGHT = 0.36f

/**
 * Right content column width weight in the landscape revamp two-pane layout (matches the Figma
 * 726/1133 split).
 */
private const val LANDSCAPE_CONTENT_WEIGHT = 0.64f

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
 * Test tag for the subscription unavailable banner (Google Play not available in region)
 */
internal const val TEST_TAG_SUBSCRIPTION_UNAVAILABLE_BANNER =
    "choose_account_screen:subscription_unavailable_banner"

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

