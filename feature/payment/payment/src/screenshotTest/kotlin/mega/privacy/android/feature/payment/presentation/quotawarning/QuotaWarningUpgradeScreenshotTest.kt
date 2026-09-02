package mega.privacy.android.feature.payment.presentation.quotawarning

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType

/**
 * Screenshot tests for the transfer quota running low warning, one per account tier. The copy
 * varies by tier rather than by trigger, so [QuotaWarningTrigger.General] covers every entry
 * point into the screen.
 *
 * Free and Pro are given a recommended subscription so the plan card and its "Best for you" badge
 * are covered too. The prices come from a fixed fixture rather than live pricing, so the reference
 * images stay stable. The highest plan is left without one, since it is offered no upgrade.
 */
class QuotaWarningUpgradeScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TransferRunningLowFreeUser() {
        AndroidThemeForPreviews {
            RunningLowWarning(runningLowState(currentPlan = AccountType.FREE))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TransferRunningLowLoggedOutUser() {
        AndroidThemeForPreviews {
            RunningLowWarning(runningLowState(currentPlan = null).copy(isLoggedIn = false))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TransferRunningLowProUser() {
        AndroidThemeForPreviews {
            RunningLowWarning(
                runningLowState(currentPlan = AccountType.PRO_I).copy(
                    transferUsed = 2_529_237_222_195,
                    transferTotal = 2_638_827_906_662,
                    transferUsedPercentage = 95,
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TransferRunningLowHighestPlanUser() {
        AndroidThemeForPreviews {
            RunningLowWarning(
                runningLowState(currentPlan = AccountType.PRO_III).copy(
                    transferUsed = 256_186_559_234_048,
                    transferTotal = 263_882_790_666_240,
                    transferUsedPercentage = 97,
                    isHighestPlan = true,
                    recommendedSubscription = null,
                )
            )
        }
    }

    @Composable
    private fun RunningLowWarning(state: QuotaWarningUpgradeState) {
        QuotaWarningUpgradeScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.General,
            uiState = state,
            onUpgradeClick = {},
            onViewAllPlansClick = {},
            onLearnMoreClick = {},
            onContactSupportClick = {},
            onManagePlanClick = {},
            onRetryClick = {},
            onClose = {},
        )
    }

    private fun runningLowState(currentPlan: AccountType?) = QuotaWarningUpgradeState(
        currentPlan = currentPlan,
        isTransferOverQuota = false,
        isLoading = false,
        recommendedSubscription = recommendedProI,
    )

    private companion object {
        val recommendedProI = LocalisedSubscription(
            monthlySubscription = Subscription(
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 2048,
                amount = CurrencyAmount(9.99F, Currency("EUR")),
                sku = "mega.android.pro1.onemonth",
            ),
            yearlySubscription = Subscription(
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 24576,
                amount = CurrencyAmount(99.99F, Currency("EUR")),
                sku = "mega.android.pro1.oneyear",
            ),
            localisedPriceCurrencyCode = LocalisedPriceCurrencyCodeStringMapper(),
            formattedSize = FormattedSizeMapper(),
        )
    }
}
