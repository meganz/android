package mega.privacy.android.feature.myaccount.presentation.usage.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.feature.myaccount.presentation.usage.MyAccountUsageUiState
import mega.privacy.android.feature.myaccount.presentation.usage.PaymentAlertType
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screenshot tests for the account usage screen, covering the states that surface each of its
 * labels: the quota meters and storage breakdown on a paid account, the two business payment
 * alerts, the load-failure dialog, and the two subscription notices that carry no date.
 *
 * Notices that embed a date ("Renews tomorrow, Aug 26, 2026", "Expires on Sep 23, 2026") are
 * deliberately left out: their reference image would go stale every day. "Renews today" and
 * "Expired yesterday" carry no date, so they are stable. The dated states exist as previews on
 * [MyAccountUsageScreen] for inspection.
 */
class MyAccountUsageScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyAccountUsagePaidAccount() {
        AndroidThemeForPreviews {
            MyAccountUsageScreen(uiState = proState)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyAccountUsageBusinessGracePeriod() {
        AndroidThemeForPreviews {
            MyAccountUsageScreen(uiState = businessState(PaymentAlertType.BusinessGracePeriod))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyAccountUsageBusinessExpired() {
        AndroidThemeForPreviews {
            MyAccountUsageScreen(uiState = businessState(PaymentAlertType.BusinessExpired))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyAccountUsageLoadFailed() {
        AndroidThemeForPreviews {
            MyAccountUsageScreen(uiState = proState.copy(usageLoadFailed = true))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyAccountUsageRenewsToday() {
        AndroidThemeForPreviews {
            MyAccountUsageScreen(
                uiState = proState.copy(
                    hasRenewableSubscription = true,
                    paymentAlertType = PaymentAlertType.AccountRenewsOn,
                    paymentAlertDate = daysFromNow(0),
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyAccountUsageExpiredYesterday() {
        AndroidThemeForPreviews {
            MyAccountUsageScreen(
                uiState = proState.copy(
                    hasExpirableSubscription = true,
                    paymentAlertType = PaymentAlertType.AccountExpiresOn,
                    paymentAlertDate = daysFromNow(-1),
                )
            )
        }
    }

    /** Midday [days] from now, so the alert lands on that calendar day in any time zone. */
    private fun daysFromNow(days: Long): Long =
        LocalDate.now(ZoneId.systemDefault())
            .plusDays(days)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()

    private val proState = MyAccountUsageUiState(
        isUsageContentReady = true,
        accountType = AccountType.PRO_I,
        usedStorage = 18 * GIGABYTE,
        totalStorage = 400 * GIGABYTE,
        usedStoragePercentage = 75,
        usedTransfer = 120 * GIGABYTE,
        totalTransfer = 400 * GIGABYTE,
        usedTransferPercentage = 30,
        usedTransferStatus = UsedTransferStatus.NoTransferProblems,
        cloudStorage = 10 * GIGABYTE,
        incomingStorage = 5 * GIGABYTE,
        rubbishStorage = 2 * GIGABYTE,
        backupStorageSize = 1 * GIGABYTE,
        isFileVersioningEnabled = true,
        versionsSize = 200 * MEGABYTE,
    )

    private fun businessState(paymentAlertType: PaymentAlertType) = MyAccountUsageUiState(
        isUsageContentReady = true,
        isBusinessAccount = true,
        isMasterBusinessAccount = true,
        usedStorage = 340 * GIGABYTE,
        usedTransfer = 80 * GIGABYTE,
        paymentAlertType = paymentAlertType,
    )

    private companion object {
        const val MEGABYTE = 1024L * 1024
        const val GIGABYTE = MEGABYTE * 1024
    }
}
