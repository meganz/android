package mega.privacy.android.app.upgradeAccount

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.ProFeature
import mega.privacy.android.app.upgradeAccount.model.extensions.toUIAccountType
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.components.AdditionalBenefitProPlanView
import mega.privacy.android.feature.payment.components.BuyPlanBottomBar
import mega.privacy.android.feature.payment.components.ChooseAccountScreenTopBar
import mega.privacy.android.feature.payment.components.FreePlanCard
import mega.privacy.android.feature.payment.components.NewFeatureRow
import mega.privacy.android.feature.payment.components.ProPlanCard
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale

@Composable
internal fun NewChooseAccountScreen(
    uiState: ChooseAccountState = ChooseAccountState(),
    onBuyPlanClick: (AccountType, Boolean) -> Unit,
    maybeLaterClicked: () -> Unit,
    onFreePlanClicked: () -> Unit,
) {
    var chosenPlan by rememberSaveable { mutableStateOf<AccountType?>(null) }
    var isMonthly by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()
    val topBarHeightPx =
        with(LocalDensity.current) { 56.dp.roundToPx() + WindowInsets.statusBars.getTop(this) }
    val headerHeightPx = with(LocalDensity.current) { 180.dp.roundToPx() }
    val position by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val itemOffset by remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }
    val currentHeaderHeightPx = headerHeightPx - itemOffset
    val transparent = position == 0 && currentHeaderHeightPx > topBarHeightPx
    val alpha by animateFloatAsState(targetValue = if (transparent) 0f else 1f)

    val proFeatures = remember {
        listOf(
            ProFeature(
                icon = IconPackR.drawable.ic_cloud,
                title = context.getString(sharedR.string.pro_plan_feature_storage_title),
                description = context.getString(sharedR.string.pro_plan_feature_storage_desc),
                testTag = "pro_plan:feature:storage"
            ),
            ProFeature(
                icon = IconPackR.drawable.ic_arrows_up_down,
                title = context.getString(sharedR.string.pro_plan_feature_transfer_title),
                description = context.getString(sharedR.string.pro_plan_feature_transfer_desc),
                testTag = "pro_plan:feature:transfer"
            ),
            ProFeature(
                icon = IconPackR.drawable.ic_vpn,
                title = context.getString(sharedR.string.pro_plan_feature_vpn_title),
                description = context.getString(sharedR.string.pro_plan_feature_vpn_desc),
                testTag = "pro_plan:feature:vpn"
            ),
            ProFeature(
                icon = IconPackR.drawable.ic_password_manager,
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
                onClick = maybeLaterClicked
            )
        },
        bottomBar = {
            chosenPlan?.let {
                BuyPlanBottomBar(
                    modifier = Modifier,
                    text = stringResource(it.toUIAccountType().textBuyButtonValue),
                    onClick = { onBuyPlanClick(it, isMonthly) },
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .testTag(TEST_TAG_LAZY_COLUMN)
                .navigationBarsPadding()
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
                    text = "Get more with Pro plan",
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
                    painter = painterResource(id = feature.icon),
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
                        text = stringResource(id = R.string.subscription_type_monthly),
                    )

                    MegaChip(
                        modifier = Modifier.testTag(TEST_TAG_YEARLY_CHIP),
                        selected = !isMonthly,
                        onClick = {
                            isMonthly = false
                        },
                        text = stringResource(id = R.string.subscription_type_yearly),
                    )
                }
            }

            item("save_up_to_badge") {
                Badge(
                    badgeType = BadgeType.Mega,
                    text = stringResource(id = sharedR.string.account_upgrade_account_label_save_up_to),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(TEST_TAG_SAVE_UP_TO_BADGE)
                )
            }

            item("pro_plans_top_space") {
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(uiState.localisedSubscriptionsList) { index, subscription ->
                val isRecommended =
                    when (uiState.cheapestSubscriptionAvailable?.accountType) {
                        AccountType.PRO_LITE -> subscription.accountType == AccountType.PRO_I
                        AccountType.PRO_I -> subscription.accountType == AccountType.PRO_II
                        AccountType.PRO_II -> subscription.accountType == AccountType.PRO_III
                        else -> false
                    }

                val storageValueString =
                    stringResource(
                        id = subscription.formatStorageSize().unit,
                        subscription.formatStorageSize().size
                    )
                val transferValueString =

                    stringResource(
                        id = subscription.formatTransferSize(isMonthly).unit,
                        subscription.formatTransferSize(isMonthly).size
                    )

                val uiAccountType = subscription.accountType.toUIAccountType()

                val storageString = stringResource(
                    id = sharedR.string.choose_account_screen_storage_label,
                    storageValueString
                )
                val transferString = stringResource(
                    id = sharedR.string.choose_account_screen_transfer_quota_label,
                    transferValueString
                )
                val totalPrice =
                    subscription.localisePriceCurrencyCode(Locale.getDefault(), isMonthly)

                val yearlyPricePerMonth = if (!isMonthly) {
                    subscription.localisePriceOfYearlyAmountPerMonth(
                        Locale.getDefault()
                    )
                } else null

                val billingInfo = if (isMonthly) {
                    stringResource(
                        sharedR.string.choose_account_screen_currency_per_month,
                        totalPrice.currencyCode
                    )
                } else {
                    stringResource(
                        sharedR.string.choose_account_screen_billed_yearly,
                        totalPrice.price
                    )
                }

                ProPlanCard(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .testTag("$TEST_TAG_PRO_PLAN_CARD$index"),
                    planName = stringResource(id = uiAccountType.textValue),
                    isRecommended = isRecommended,
                    isSelected = chosenPlan == subscription.accountType,
                    storage = storageString,
                    transfer = transferString,
                    price = yearlyPricePerMonth?.price ?: totalPrice.price,
                    priceUnit = stringResource(sharedR.string.general_month).takeIf { !isMonthly }
                        .orEmpty(),
                    billingInfo = billingInfo,
                    onSelected = { chosenPlan = subscription.accountType },
                )

                if (index < uiState.localisedSubscriptionsList.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item("pro_plans_bottom_space") {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item("additional_benefits") {
                AdditionalBenefitProPlanView(
                    title = stringResource(id = sharedR.string.pro_plan_additional_benefits_section_title),
                    benefits = listOf(
                        stringResource(id = sharedR.string.pro_plan_benefit_password_protected_links),
                        stringResource(id = sharedR.string.pro_plan_benefit_links_with_expiry_dates),
                        stringResource(id = sharedR.string.pro_plan_benefit_transfer_quota_sharing),
                        stringResource(id = sharedR.string.pro_plan_benefit_auto_sync_mobile),
                        stringResource(id = sharedR.string.pro_plan_benefit_rewind_180_days),
                        stringResource(id = sharedR.string.pro_plan_benefit_host_calls_unlimited),
                        stringResource(id = sharedR.string.pro_plan_benefit_schedule_rubbish_bin_clearing),
                        stringResource(id = sharedR.string.pro_plan_benefit_priority_support),
                    ),
                    modifier = Modifier.testTag(TEST_TAG_ADDITIONAL_BENEFITS)
                )
            }

            item("free_plan_card") {
                FreePlanCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag(TEST_TAG_FREE_PLAN_CARD),
                    onContinue = onFreePlanClicked,
                )
            }

            item("subscription_info") {
                MegaText(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(TEST_TAG_SUBSCRIPTION_INFO_TITLE),
                    text = stringResource(id = sharedR.string.choose_account_screen_subscription_information_title),
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
                    value = stringResource(id = sharedR.string.choose_account_screen_subscription_information_description),
                    spanStyles = mapOf(
                        SpanIndicator('A') to SpanStyleWithAnnotation(
                            megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                                spanStyle = SpanStyle(),
                                linkColor = LinkColor.Primary
                            ),
                            annotation = stringResource(id = sharedR.string.choose_account_screen_subscription_information_description)
                                .substringAfter("[A]")
                                .substringBefore("[/A]")
                        )
                    ),
                    baseTextColor = TextColor.Secondary,
                    baseStyle = AppTheme.typography.bodySmall,
                    onAnnotationClick = {
                    }
                )
                LinkSpannedText(
                    modifier = Modifier
                        .padding(
                            top = LocalSpacing.current.x24,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16,
                            bottom = LocalSpacing.current.x48
                        )
                        .testTag(TEST_TAG_TERMS_AND_POLICIES),
                    value = stringResource(id = sharedR.string.choose_account_screen_terms_and_policies_link_text),
                    spanStyles = mapOf(
                        SpanIndicator('A') to SpanStyleWithAnnotation(
                            megaSpanStyle = MegaSpanStyle.LinkColorStyle(
                                spanStyle = SpanStyle(),
                                linkColor = LinkColor.Primary
                            ),
                            annotation = stringResource(id = sharedR.string.choose_account_screen_terms_and_policies_link_text)
                                .substringAfter("[A]")
                                .substringBefore("[/A]")
                        )
                    ),
                    baseStyle = AppTheme.typography.labelLarge,
                    onAnnotationClick = {
                    }
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun NewChooseAccountScreenPreview(
    @PreviewParameter(ChooseAccountPreviewProvider::class) state: ChooseAccountState,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        NewChooseAccountScreen(
            uiState = state,
            onBuyPlanClick = { _, _ -> },
            onFreePlanClicked = {},
            maybeLaterClicked = {}
        )
    }
}

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
 * Test tag prefix for each Pro plan card (append index)
 */
internal const val TEST_TAG_PRO_PLAN_CARD = "choose_account_screen:pro_plan_card_"

/**
 * Test tag for the additional benefits section
 */
internal const val TEST_TAG_ADDITIONAL_BENEFITS = "choose_account_screen:additional_benefits"

/**
 * Test tag for the Free plan card
 */
internal const val TEST_TAG_FREE_PLAN_CARD = "choose_account_screen:free_plan_card"

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