package mega.privacy.android.feature.payment.quotawarning

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.payment.components.QuotaUsageLevel
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaMetric
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningMessageMapper
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.Test

class QuotaWarningMessageMapperTest {

    private val underTest = QuotaWarningMessageMapper()

    @Test
    fun `test that Orange storage maps to almost full copy and warning level`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            storageState = StorageState.Orange,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.titleId)
            .isEqualTo(sharedR.string.subscription_quota_storage_almost_full_title)
        assertThat(result.titleTakesPercentage).isTrue()
        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_storage_almost_full_subtitle)
        assertThat(result.metric).isEqualTo(QuotaMetric.Storage)
        assertThat(result.showLearnMore).isFalse()
        assertThat(result.level).isEqualTo(QuotaUsageLevel.Warning)
    }

    @Test
    fun `test that Red storage on upload maps to the run-out-of-space subtitle and error level`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            storageState = StorageState.Red,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_storage_over_upload_subtitle)
        assertThat(result.level).isEqualTo(QuotaUsageLevel.Error)
    }

    @Test
    fun `test that full storage on a general trigger maps to the general full subtitle`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.General,
            storageState = StorageState.PayWall,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_storage_over_subtitle)
        assertThat(result.level).isEqualTo(QuotaUsageLevel.Error)
    }

    @Test
    fun `test that free user transfer running low on download maps to running low title and warning`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            storageState = StorageState.Unknown,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.titleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_low_title)
        assertThat(result.titleTakesPercentage).isFalse()
        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_low_download_subtitle)
        assertThat(result.metric).isEqualTo(QuotaMetric.Transfer)
        assertThat(result.showLearnMore).isTrue()
        assertThat(result.level).isEqualTo(QuotaUsageLevel.Warning)
    }

    @Test
    fun `test that pro user transfer running low maps to the percentage used title`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            storageState = StorageState.Unknown,
            isTransferOverQuota = false,
            isProUser = true,
        )

        assertThat(result.titleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_percentage_used_title)
        assertThat(result.titleTakesPercentage).isTrue()
    }

    @Test
    fun `test that transfer running low on streaming maps to the streaming subtitle`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Streaming,
            storageState = StorageState.Unknown,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_low_streaming_subtitle)
    }

    @Test
    fun `test that transfer over quota on download maps to exceeded title and error level`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            storageState = StorageState.Unknown,
            isTransferOverQuota = true,
            isProUser = true,
        )

        assertThat(result.titleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_over_title)
        assertThat(result.titleTakesPercentage).isFalse()
        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_over_download_subtitle)
        assertThat(result.showLearnMore).isTrue()
        assertThat(result.level).isEqualTo(QuotaUsageLevel.Error)
    }

    @Test
    fun `test that highest plan storage maps to the manage-plan subtitle with a link`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            storageState = StorageState.Red,
            isTransferOverQuota = false,
            isProUser = true,
            isHighestPlan = true,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_storage_highest_plan_subtitle)
        assertThat(result.subtitleHasLink).isTrue()
        assertThat(result.showLearnMore).isFalse()
        assertThat(result.titleId)
            .isEqualTo(sharedR.string.subscription_quota_storage_almost_full_title)
    }

    @Test
    fun `test that highest plan transfer running low on download maps to the manage-plan subtitle`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            storageState = StorageState.Unknown,
            isTransferOverQuota = false,
            isProUser = true,
            isHighestPlan = true,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_low_download_highest_plan_subtitle)
        assertThat(result.subtitleHasLink).isTrue()
        assertThat(result.showLearnMore).isFalse()
    }

    @Test
    fun `test that highest plan transfer over quota on streaming maps to the manage-plan subtitle`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Streaming,
            storageState = StorageState.Unknown,
            isTransferOverQuota = true,
            isProUser = true,
            isHighestPlan = true,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_over_streaming_highest_plan_subtitle)
        assertThat(result.subtitleHasLink).isTrue()
    }

    @Test
    fun `test that transfer over quota on streaming maps to the streaming exceeded subtitle`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Streaming,
            storageState = StorageState.Unknown,
            isTransferOverQuota = true,
            isProUser = false,
        )

        assertThat(result.subtitleId)
            .isEqualTo(sharedR.string.subscription_quota_transfer_over_streaming_subtitle)
        assertThat(result.level).isEqualTo(QuotaUsageLevel.Error)
    }
}
