package mega.privacy.android.app.upgradeAccount.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider.Companion.localisedSubscriptionsList
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider.Companion.subscriptionProLite
import mega.privacy.android.app.upgradeAccount.view.components.ChoosePlanTitleText
import mega.privacy.android.app.upgradeAccount.view.components.FeatureRow
import mega.privacy.android.app.upgradeAccount.view.components.GetProPlanColumn
import mega.privacy.android.app.upgradeAccount.view.components.MonthlyYearlyTabs
import mega.privacy.android.app.upgradeAccount.view.components.ProPlanInfoCard
import mega.privacy.android.app.upgradeAccount.view.components.SaveUpToLabel
import mega.privacy.android.app.upgradeAccount.view.components.SubscriptionDetails
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme


/**
 *  Compose UI for new Onboarding dialog (Choose account screen), this is Variant B
 *  User will see this screen when the registration was finished and user signs in for the first time ever
 */
@Composable
fun VariantBOnboardingDialogView(
    state: ChooseAccountState,
    onBackPressed: () -> Unit,
    onContinueClicked: () -> Unit,
    onChoosingMonthlyYearlyPlan: (isMonthly: Boolean) -> Unit,
    onChoosingPlanType: (chosenPlan: AccountType) -> Unit,
    onPlayStoreLinkClicked: (String) -> Unit,
    onProIIIVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = onBackPressed,
                title = stringResource(id = sharedR.string.dialog_onboarding_app_bar_title),
            )
        },
        content = {
            VariantBOnboardingDialogColumn(
                uiState = state,
                scrollState = scrollState,
                onContinueClicked = onContinueClicked,
                onChoosingMonthlyYearlyPlan = onChoosingMonthlyYearlyPlan,
                onChoosingPlanType = onChoosingPlanType,
                onPlayStoreLinkClicked = onPlayStoreLinkClicked,
                onProIIIVisible = onProIIIVisible,
                modifier = modifier,
            )
        }
    )
}

@Composable
internal fun VariantBOnboardingDialogColumn(
    uiState: ChooseAccountState,
    scrollState: ScrollState,
    onContinueClicked: () -> Unit,
    onChoosingMonthlyYearlyPlan: (isMonthly: Boolean) -> Unit,
    onChoosingPlanType: (chosenPlan: AccountType) -> Unit,
    onPlayStoreLinkClicked: (String) -> Unit,
    onProIIIVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMonthly by rememberSaveable { mutableStateOf(false) }
    val cheapestSubscriptionAvailable = uiState.cheapestSubscriptionAvailable
    val isLoading = cheapestSubscriptionAvailable == null
    val formattedStorage = cheapestSubscriptionAvailable?.formatStorageSize(usePlaceholder = false)
    val minimalStorageUnitString = formattedStorage?.let { stringResource(id = it.unit) } ?: ""
    val minimalStorageSizeString = formattedStorage?.size ?: ""
    var chosenPlan by rememberSaveable { mutableStateOf(AccountType.PRO_I) }
    var isPreselectedPlanOnce by rememberSaveable { mutableStateOf(false) }
    val isPaymentMethodAvailable = uiState.isPaymentMethodAvailable
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    var isProIIIPlanCardViewed by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            Column(
                modifier = modifier
                    .width(390.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                GetProPlanColumn(
                    state = uiState,
                    isLoading = isLoading,
                    bodyTextStyle = MaterialTheme.typography.h6,
                )
                Spacer(modifier = Modifier.height(16.dp))
                //Storage
                FeatureRow(
                    drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
                    title = stringResource(id = R.string.dialog_onboarding_feature_title_storage),
                    description = stringResource(
                        id = sharedR.string.dialog_onboarding_feature_storage_description,
                        minimalStorageSizeString,
                        minimalStorageUnitString
                    ),
                    testTag = STORAGE_DESCRIPTION_ROW,
                    isLoading = isLoading,
                )
                //File sharing
                FeatureRow(
                    drawableID = painterResource(id = R.drawable.ic_file_sharing_onboarding_dialog),
                    title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_file_sharing),
                    description = stringResource(id = sharedR.string.dialog_onboarding_feature_description_file_sharing),
                    testTag = FILE_SHARING_DESCRIPTION_ROW,
                    isLoading = isLoading,
                )
                //Back-up and rewind
                FeatureRow(
                    drawableID = painterResource(id = R.drawable.ic_backup_onboarding_dialog),
                    title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_backup_rewind),
                    description = stringResource(id = sharedR.string.dialog_onboarding_feature_description_backup_rewind),
                    testTag = BACKUP_DESCRIPTION_ROW,
                    isLoading = isLoading,
                )
                //Extra features
                FeatureRow(
                    drawableID = painterResource(id = R.drawable.ic_mega_onboarding_dialog),
                    title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_additional_features),
                    description = stringResource(
                        id = if (uiState.showAdsFeature) sharedR.string.dialog_onboarding_feature_description_additional_features_with_ads else sharedR.string.dialog_onboarding_feature_description_additional_features_without_ads
                    ),
                    testTag = ADDITIONAL_FEATURES_DESCRIPTION_ROW,
                    isLoading = isLoading,
                    isBulletPointListUsed = true,
                )
            }
        }
        Row(
            modifier = modifier.padding(top = 16.dp),
        ) {
            Column(
                modifier = modifier.width(390.dp)
            ) {
                ChoosePlanTitleText(testTag = ONBOARDING_SCREEN_VARIANT_B)
                MonthlyYearlyTabs(
                    isMonthly = isMonthly,
                    onTabClicked = {
                        isMonthly = it
                        onChoosingMonthlyYearlyPlan(it)
                    },
                    testTag = ONBOARDING_SCREEN_VARIANT_B,
                )
                SaveUpToLabel()
                if (uiState.localisedSubscriptionsList.isEmpty()) {
                    LoadingShimmerEffect()
                } else {
                    ProPlanInfoCard(
                        proPlan = AccountType.FREE,
                        subscription = cheapestSubscriptionAvailable!!,
                        isRecommended = false,
                        onPlanClicked = {
                            chosenPlan = AccountType.FREE
                            isPreselectedPlanOnce = true
                            onChoosingPlanType(AccountType.FREE)
                            onChoosingMonthlyYearlyPlan(isMonthly)
                        },
                        isMonthly = isMonthly,
                        isClicked = chosenPlan == AccountType.FREE,
                        showCurrentPlanLabel = false,
                        testTag = PRO_PLAN_CARD_VARIANT_B,
                    )
                    uiState.localisedSubscriptionsList.forEach { localisedSubscription ->
                        val isRecommended =
                            when (uiState.cheapestSubscriptionAvailable.accountType) {
                                AccountType.PRO_LITE -> localisedSubscription.accountType == AccountType.PRO_I
                                AccountType.PRO_I -> localisedSubscription.accountType == AccountType.PRO_II
                                AccountType.PRO_II -> localisedSubscription.accountType == AccountType.PRO_III
                                else -> false
                            }
                        val disableCardClick = false
                        val isClicked =
                            (chosenPlan == localisedSubscription.accountType) && isPaymentMethodAvailable && !disableCardClick
                        if (isRecommended && !isPreselectedPlanOnce && isPaymentMethodAvailable) {
                            chosenPlan = localisedSubscription.accountType
                            onChoosingMonthlyYearlyPlan(isMonthly)
                            onChoosingPlanType(localisedSubscription.accountType)
                        }
                        ProPlanInfoCard(
                            proPlan = localisedSubscription.accountType,
                            subscription = localisedSubscription,
                            isRecommended = isRecommended,
                            onPlanClicked = {
                                if (!isPaymentMethodAvailable) {
                                    chosenPlan = AccountType.FREE
                                    onChoosingPlanType(AccountType.FREE)
                                    onChoosingMonthlyYearlyPlan(isMonthly)
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(0)
                                    }
                                }
                                if (isPaymentMethodAvailable) {
                                    if (!disableCardClick) {
                                        chosenPlan = localisedSubscription.accountType
                                        isPreselectedPlanOnce = true
                                        onChoosingMonthlyYearlyPlan(isMonthly)
                                        onChoosingPlanType(localisedSubscription.accountType)
                                    }
                                }
                            },
                            isMonthly = isMonthly,
                            isClicked = isClicked,
                            showCurrentPlanLabel = false,
                            testTag = PRO_PLAN_CARD_VARIANT_B,
                            modifier = Modifier
                                .onGloballyPositioned { layoutCoordinates ->
                                    if (localisedSubscription.accountType == AccountType.PRO_III && !isProIIIPlanCardViewed) {
                                        with(density) {
                                            // height of the Pro III plan card
                                            val proIIIPlanCardHeight = layoutCoordinates.size.height
                                            // vertical position of the Pro III plan card in Compose
                                            val proIIIPlanCardVerticalPosition =
                                                layoutCoordinates.positionInRoot().y
                                            // vertical position Pro III plan card in Compose including its height
                                            val proIIICardFullVerticalPosition =
                                                proIIIPlanCardVerticalPosition.toDp().value.toInt() + proIIIPlanCardHeight.toDp().value.toInt()
                                            val screenHeight =
                                                configuration.screenHeightDp
                                            val isFullyVisible =
                                                proIIICardFullVerticalPosition <= screenHeight
                                            if (isFullyVisible && !isProIIIPlanCardViewed) {
                                                onProIIIVisible()
                                                isProIIIPlanCardViewed = true
                                            }
                                        }
                                    }
                                }
                        )
                    }
                    RaisedDefaultMegaButton(
                        textId = sharedR.string.dialog_onboarding_button_continue,
                        onClick = onContinueClicked,
                        modifier = Modifier
                            .padding(17.dp)
                            .fillMaxWidth()
                            .testTag(CONTINUE_BUTTON),
                    )

                    if (uiState.localisedSubscriptionsList.isNotEmpty()) {
                        SubscriptionDetails(
                            onLinkClick = onPlayStoreLinkClicked,
                            chosenPlan = chosenPlan,
                            subscriptionList = uiState.localisedSubscriptionsList,
                            isMonthly = isMonthly,
                        )
                    }
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewVariantBOnboardingDialogView(
    @PreviewParameter(VariantBOnboardingDialogPreviewProvider::class) state: ChooseAccountState,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VariantBOnboardingDialogView(
            state = state,
            onBackPressed = {},
            onContinueClicked = {},
            onChoosingMonthlyYearlyPlan = {},
            onChoosingPlanType = {},
            onPlayStoreLinkClicked = {},
            onProIIIVisible = {},
        )
    }
}

private class VariantBOnboardingDialogPreviewProvider :
    PreviewParameterProvider<ChooseAccountState> {
    override val values: Sequence<ChooseAccountState>
        get() = sequenceOf(
            ChooseAccountState(
                cheapestSubscriptionAvailable = subscriptionProLite,
                localisedSubscriptionsList = localisedSubscriptionsList,
            )
        )
}

internal const val ONBOARDING_SCREEN_VARIANT_B = "onboarding_screen_variant_b:"
internal const val PRO_PLAN_CARD_VARIANT_B = "onboarding_screen_variant_b:card_pro_plan_"
internal const val CONTINUE_BUTTON = "onboarding_screen_variant_b:continue_button"