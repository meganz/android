package mega.privacy.android.feature.payment.quotawarning

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningEventMapper
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.mobile.analytics.event.StorageAlmostFullFreeUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.StorageAlmostFullFreeUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageAlmostFullFreeUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageAlmostFullProUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.StorageAlmostFullProUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageAlmostFullProUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageFullFreeUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.StorageFullFreeUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageFullFreeUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageFullProUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.StorageFullProUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.StorageFullProUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedFreeUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedFreeUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedFreeUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedNotLoggedInUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedNotLoggedInUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedNotLoggedInUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedProUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedProUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAllUsedProUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedFreeUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedFreeUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedFreeUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedNotLoggedInUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedNotLoggedInUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedNotLoggedInUserViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedProUserDialogScreenEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedProUserUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferAlmostUsedProUserViewAllPlansButtonPressedEvent
import org.junit.jupiter.api.Test

class QuotaWarningEventMapperTest {

    private val underTest = QuotaWarningEventMapper()

    @Test
    fun `test that free user almost full storage maps to the storage almost full free user events`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            storageState = StorageState.Orange,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.screenView).isEqualTo(StorageAlmostFullFreeUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(StorageAlmostFullFreeUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(StorageAlmostFullFreeUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that pro user almost full storage maps to the storage almost full pro user events`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            storageState = StorageState.Orange,
            isTransferOverQuota = false,
            isProUser = true,
        )

        assertThat(result.screenView).isEqualTo(StorageAlmostFullProUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(StorageAlmostFullProUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(StorageAlmostFullProUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that free user full storage maps to the storage full free user events`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            storageState = StorageState.Red,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.screenView).isEqualTo(StorageFullFreeUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(StorageFullFreeUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(StorageFullFreeUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that pro user full storage maps to the storage full pro user events`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            storageState = StorageState.PayWall,
            isTransferOverQuota = false,
            isProUser = true,
        )

        assertThat(result.screenView).isEqualTo(StorageFullProUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(StorageFullProUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(StorageFullProUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that free user transfer running low maps to the transfer almost used free user events`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Green,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.screenView).isEqualTo(TransferAlmostUsedFreeUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(TransferAlmostUsedFreeUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(TransferAlmostUsedFreeUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that pro user transfer running low maps to the transfer almost used pro user events`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Green,
            isTransferOverQuota = false,
            isProUser = true,
        )

        assertThat(result.screenView).isEqualTo(TransferAlmostUsedProUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(TransferAlmostUsedProUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(TransferAlmostUsedProUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that free user transfer over quota maps to the transfer all used free user events`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Green,
            isTransferOverQuota = true,
            isProUser = false,
        )

        assertThat(result.screenView).isEqualTo(TransferAllUsedFreeUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(TransferAllUsedFreeUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(TransferAllUsedFreeUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that pro user transfer over quota maps to the transfer all used pro user events`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Green,
            isTransferOverQuota = true,
            isProUser = true,
        )

        assertThat(result.screenView).isEqualTo(TransferAllUsedProUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(TransferAllUsedProUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(TransferAllUsedProUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that not logged in transfer running low maps to the transfer almost used events`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Unknown,
            isTransferOverQuota = false,
            isProUser = false,
            isLoggedIn = false,
        )

        assertThat(result.screenView).isEqualTo(TransferAlmostUsedNotLoggedInUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(TransferAlmostUsedNotLoggedInUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(TransferAlmostUsedNotLoggedInUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that not logged in transfer over quota maps to the transfer all used events`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Unknown,
            isTransferOverQuota = true,
            isProUser = false,
            isLoggedIn = false,
        )

        assertThat(result.screenView).isEqualTo(TransferAllUsedNotLoggedInUserDialogScreenEvent)
        assertThat(result.upgradeButtonPressed)
            .isEqualTo(TransferAllUsedNotLoggedInUserUpgradeButtonPressedEvent)
        assertThat(result.viewAllPlansButtonPressed)
            .isEqualTo(TransferAllUsedNotLoggedInUserViewAllPlansButtonPressedEvent)
    }

    @Test
    fun `test that storage severity ignores the transfer over quota flag`() {
        val result = underTest(
            type = QuotaWarningType.Storage,
            storageState = StorageState.Orange,
            isTransferOverQuota = true,
            isProUser = false,
        )

        assertThat(result.screenView).isEqualTo(StorageAlmostFullFreeUserDialogScreenEvent)
    }

    @Test
    fun `test that transfer severity ignores the storage state`() {
        val result = underTest(
            type = QuotaWarningType.Transfer,
            storageState = StorageState.Red,
            isTransferOverQuota = false,
            isProUser = false,
        )

        assertThat(result.screenView).isEqualTo(TransferAlmostUsedFreeUserDialogScreenEvent)
    }
}
