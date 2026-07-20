package mega.privacy.android.feature.payment.presentation.quotawarning

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.payment.components.QuotaUsageLevel
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * The usage metric a quota-warning scenario is about.
 */
enum class QuotaMetric {
    /**
     * Storage usage.
     */
    Storage,

    /**
     * Transfer quota usage.
     */
    Transfer,
}

/**
 * Title, subtitle, metric and severity shown on the quota-warning upsell screen.
 *
 * @property titleId title string resource
 * @property titleTakesPercentage whether [titleId] takes the usage percentage as a format argument
 * @property subtitleId subtitle string resource
 * @property showLearnMore whether the subtitle is followed by a "Learn more" link
 * @property metric which usage metric (storage or transfer) the cards display
 * @property level severity of the current-plan usage bar, derived from the backend quota state
 */
data class QuotaWarningMessage(
    @StringRes val titleId: Int,
    val titleTakesPercentage: Boolean,
    @StringRes val subtitleId: Int,
    val showLearnMore: Boolean,
    val metric: QuotaMetric,
    val level: QuotaUsageLevel,
)

/**
 * Maps the caller-provided [QuotaWarningType] (metric) and [QuotaWarningTrigger] (action) together
 * with the backend quota state to the copy and severity shown on the quota-warning upsell screen.
 *
 * Severity is never computed from usage percentages: storage almost-full vs full comes from the
 * backend [StorageState], and transfer running-low vs exceeded comes from the backend
 * transfer over-quota flag.
 */
class QuotaWarningMessageMapper @Inject constructor() {
    /**
     * @param type the quota metric (storage or transfer) the screen was opened for
     * @param trigger the user action that triggered the screen (download, upload or streaming)
     * @param storageState the backend storage state, used for storage warnings
     * @param isTransferOverQuota whether the backend reports the transfer quota as exceeded
     * @param isProUser whether the current account is a paid (Pro) plan
     * @return the copy, metric and severity to show
     */
    operator fun invoke(
        type: QuotaWarningType,
        trigger: QuotaWarningTrigger,
        storageState: StorageState,
        isTransferOverQuota: Boolean,
        isProUser: Boolean,
    ): QuotaWarningMessage = when (type) {
        QuotaWarningType.Storage -> storageMessage(storageState, trigger)
        QuotaWarningType.Transfer -> transferMessage(trigger, isTransferOverQuota, isProUser)
    }

    private fun storageMessage(
        storageState: StorageState,
        trigger: QuotaWarningTrigger,
    ): QuotaWarningMessage {
        val isFull = storageState == StorageState.Red || storageState == StorageState.PayWall
        val subtitleId = when {
            !isFull -> sharedR.string.subscription_quota_storage_almost_full_subtitle
            trigger == QuotaWarningTrigger.Upload ->
                sharedR.string.subscription_quota_storage_over_upload_subtitle

            else -> sharedR.string.subscription_quota_storage_over_subtitle
        }
        return QuotaWarningMessage(
            titleId = sharedR.string.subscription_quota_storage_almost_full_title,
            titleTakesPercentage = true,
            subtitleId = subtitleId,
            showLearnMore = false,
            metric = QuotaMetric.Storage,
            level = if (isFull) QuotaUsageLevel.Error else QuotaUsageLevel.Warning,
        )
    }

    private fun transferMessage(
        trigger: QuotaWarningTrigger,
        isTransferOverQuota: Boolean,
        isProUser: Boolean,
    ): QuotaWarningMessage {
        val isStreaming = trigger == QuotaWarningTrigger.Streaming
        return if (isTransferOverQuota) {
            QuotaWarningMessage(
                titleId = sharedR.string.subscription_quota_transfer_over_title,
                titleTakesPercentage = false,
                subtitleId = if (isStreaming) {
                    sharedR.string.subscription_quota_transfer_over_streaming_subtitle
                } else {
                    sharedR.string.subscription_quota_transfer_over_download_subtitle
                },
                showLearnMore = true,
                metric = QuotaMetric.Transfer,
                level = QuotaUsageLevel.Error,
            )
        } else {
            QuotaWarningMessage(
                titleId = if (isProUser) {
                    sharedR.string.subscription_quota_transfer_percentage_used_title
                } else {
                    sharedR.string.subscription_quota_transfer_low_title
                },
                titleTakesPercentage = isProUser,
                subtitleId = if (isStreaming) {
                    sharedR.string.subscription_quota_transfer_low_streaming_subtitle
                } else {
                    sharedR.string.subscription_quota_transfer_low_download_subtitle
                },
                showLearnMore = true,
                metric = QuotaMetric.Transfer,
                level = QuotaUsageLevel.Warning,
            )
        }
    }
}
