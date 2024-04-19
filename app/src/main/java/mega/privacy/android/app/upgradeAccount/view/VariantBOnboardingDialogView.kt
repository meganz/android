package mega.privacy.android.app.upgradeAccount.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider.Companion.localisedSubscriptionsList
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider.Companion.subscriptionProLite
import mega.privacy.android.app.upgradeAccount.view.components.ChoosePlanTitleText
import mega.privacy.android.app.upgradeAccount.view.components.FeatureRow
import mega.privacy.android.app.upgradeAccount.view.components.GetProPlanColumn
import mega.privacy.android.app.upgradeAccount.view.components.MonthlyYearlyTabs
import mega.privacy.android.app.upgradeAccount.view.components.ProPlanInfoCard
import mega.privacy.android.app.upgradeAccount.view.components.SaveUpToLabel
import mega.privacy.android.app.upgradeAccount.view.components.SubscriptionDetails
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.theme.MegaAppTheme


@Composable
fun VariantBOnboardingDialogView(
    state: ChooseAccountState,
    onChoosingMonthlyYearlyPlan: (isMonthly: Boolean) -> Unit,
    onChoosingPlanType: (chosenPlan: AccountType) -> Unit,
    onPlayStoreLinkClicked: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    MegaScaffold(
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = "Choose your MEGA plan",
            )
        },
        content = {
            VariantBOnboardingDialogColumn(
                uiState = state,
                scrollState = scrollState,
                onChoosingMonthlyYearlyPlan = onChoosingMonthlyYearlyPlan,
                onChoosingPlanType = onChoosingPlanType,
                onPlayStoreLinkClicked = onPlayStoreLinkClicked,
            )
        }
    )
}

@Composable
internal fun VariantBOnboardingDialogColumn(
    uiState: ChooseAccountState,
    scrollState: ScrollState,
    onChoosingMonthlyYearlyPlan: (isMonthly: Boolean) -> Unit,
    onChoosingPlanType: (chosenPlan: AccountType) -> Unit,
    onPlayStoreLinkClicked: (String) -> Unit,
) {
    var isMonthly by rememberSaveable { mutableStateOf(false) }
    val cheapestSubscriptionAvailable = uiState.cheapestSubscriptionAvailable
    val isLoading = cheapestSubscriptionAvailable == null
    val formattedStorage = cheapestSubscriptionAvailable?.formatStorageSize()
    val minimalStorageString = formattedStorage?.let { stringResource(id = it.unit, it.size) }
    var chosenPlan by rememberSaveable { mutableStateOf(AccountType.FREE) }
    var isPreselectedPlanOnce by rememberSaveable { mutableStateOf(false) }
    val isPaymentMethodAvailable = uiState.isPaymentMethodAvailable
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(390.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                    description = if (minimalStorageString != null) stringResource(
                        id = R.string.dialog_onboarding_feature_description_storage,
                        minimalStorageString
                    )
                    else stringResource(id = R.string.dialog_onboarding_feature_description_storage),
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
                //MEGA VPN
                FeatureRow(
                    drawableID = painterResource(id = R.drawable.ic_vpn_onboarding_dialog),
                    title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_vpn),
                    description = stringResource(id = sharedR.string.dialog_onboarding_feature_description_vpn),
                    testTag = VPN_DESCRIPTION_ROW,
                    isLoading = isLoading,
                )
                //Chat and meetings
                FeatureRow(
                    drawableID = painterResource(id = R.drawable.ic_chat_onboarding_dialog),
                    title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_chat),
                    description = stringResource(id = sharedR.string.dialog_onboarding_feature_description_chat),
                    testTag = CHAT_DESCRIPTION_ROW,
                    isLoading = isLoading,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Column(
                modifier = Modifier.width(390.dp)
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
                        onPlanClicked = { /*TODO*/ },
                        isMonthly = true,
                        isClicked = chosenPlan == AccountType.FREE,
                        showCurrentPlanLabel = false,
                        testTag = PRO_PLAN_CARD_VARIANT_B,
                    )
                    uiState.localisedSubscriptionsList.forEach {
                        val isRecommended =
                            it.accountType == AccountType.PRO_I
                        val disableCardClick = false
                        val isClicked =
                            (chosenPlan == it.accountType) && isPaymentMethodAvailable && !disableCardClick
                        if (isRecommended && !isPreselectedPlanOnce && isPaymentMethodAvailable) {
                            chosenPlan = it.accountType
                            onChoosingMonthlyYearlyPlan(isMonthly)
                            onChoosingPlanType(it.accountType)
                        }
                        ProPlanInfoCard(
                            proPlan = it.accountType,
                            subscription = it,
                            isRecommended = isRecommended,
                            onPlanClicked = {
                                if (!isPaymentMethodAvailable) {
                                    chosenPlan = AccountType.FREE
                                    onChoosingPlanType(it.accountType)
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(0)
                                    }
                                }
                                if (isPaymentMethodAvailable) {
                                    if (!disableCardClick) {
                                        chosenPlan = it.accountType
                                        isPreselectedPlanOnce = true
                                        onChoosingMonthlyYearlyPlan(isMonthly)
                                        onChoosingPlanType(it.accountType)
                                    }
                                }
                            },
                            isMonthly = isMonthly,
                            isClicked = isClicked,
                            showCurrentPlanLabel = false,
                            testTag = PRO_PLAN_CARD_VARIANT_B,
                        )
                    }

                    // The button will be uncommented when strings will be approved and uploaded on Transifex
//                    RaisedDefaultMegaButton(
//                        textId = sharedR.string.dialog_onboarding_variant_b_button_continue,
//                        onClick = { /*TODO*/ },
//                        modifier = Modifier
//                            .padding(17.dp)
//                            .fillMaxWidth()
//                            .testTag(CONTINUE_BUTTON),
//                    )

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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VariantBOnboardingDialogView(
            state = state,
            onChoosingMonthlyYearlyPlan = {},
            onChoosingPlanType = {},
            onPlayStoreLinkClicked = {},
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