package mega.privacy.android.feature.payment.presentation.quotawarning

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.mobile.analytics.core.event.identifier.ButtonPressedEventIdentifier
import mega.privacy.mobile.analytics.core.event.identifier.ScreenViewEventIdentifier
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
import javax.inject.Inject

/**
 * Analytics events for one quota-warning scenario and account type.
 *
 * @property screenView screen-view event for the scenario
 * @property upgradeButtonPressed event for the "Upgrade to <plan>" button
 * @property viewAllPlansButtonPressed event for the "View all plans" button
 */
internal data class QuotaWarningEvents(
    val screenView: ScreenViewEventIdentifier,
    val upgradeButtonPressed: ButtonPressedEventIdentifier,
    val viewAllPlansButtonPressed: ButtonPressedEventIdentifier,
)

/**
 * Maps the quota-warning scenario and account type to the analytics events to report. The tracking
 * spec distinguishes free from Pro users, so every scenario has a free and a Pro variant, plus a
 * not-logged-in variant for the transfer scenarios anonymous users reach from public links. Storage
 * warnings need no such variant: they are raised against an account, so there is always a session.
 */
internal class QuotaWarningEventMapper @Inject constructor() {
    /**
     * @param type the quota metric (storage or transfer) the screen was opened for
     * @param storageState the backend storage state, used for storage warnings
     * @param isTransferOverQuota whether the backend reports the transfer quota as exceeded
     * @param isProUser whether the current account is a paid (Pro) plan
     * @param isLoggedIn whether a user is signed in; anonymous users reach the transfer scenarios
     * from public links and are reported with their own events
     * @return the events matching the scenario the user is shown
     */
    operator fun invoke(
        type: QuotaWarningType,
        storageState: StorageState,
        isTransferOverQuota: Boolean,
        isProUser: Boolean,
        isLoggedIn: Boolean = true,
    ): QuotaWarningEvents = when (
        quotaWarningScenario(
            type = type,
            storageState = storageState,
            isTransferOverQuota = isTransferOverQuota,
        )
    ) {
        QuotaWarningScenario.StorageAlmostFull -> if (isProUser) {
            QuotaWarningEvents(
                screenView = StorageAlmostFullProUserDialogScreenEvent,
                upgradeButtonPressed = StorageAlmostFullProUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = StorageAlmostFullProUserViewAllPlansButtonPressedEvent,
            )
        } else {
            QuotaWarningEvents(
                screenView = StorageAlmostFullFreeUserDialogScreenEvent,
                upgradeButtonPressed = StorageAlmostFullFreeUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = StorageAlmostFullFreeUserViewAllPlansButtonPressedEvent,
            )
        }

        QuotaWarningScenario.StorageFull -> if (isProUser) {
            QuotaWarningEvents(
                screenView = StorageFullProUserDialogScreenEvent,
                upgradeButtonPressed = StorageFullProUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = StorageFullProUserViewAllPlansButtonPressedEvent,
            )
        } else {
            QuotaWarningEvents(
                screenView = StorageFullFreeUserDialogScreenEvent,
                upgradeButtonPressed = StorageFullFreeUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = StorageFullFreeUserViewAllPlansButtonPressedEvent,
            )
        }

        QuotaWarningScenario.TransferAlmostUsed -> when {
            !isLoggedIn -> QuotaWarningEvents(
                screenView = TransferAlmostUsedNotLoggedInUserDialogScreenEvent,
                upgradeButtonPressed = TransferAlmostUsedNotLoggedInUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = TransferAlmostUsedNotLoggedInUserViewAllPlansButtonPressedEvent,
            )

            isProUser -> QuotaWarningEvents(
                screenView = TransferAlmostUsedProUserDialogScreenEvent,
                upgradeButtonPressed = TransferAlmostUsedProUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = TransferAlmostUsedProUserViewAllPlansButtonPressedEvent,
            )

            else -> QuotaWarningEvents(
                screenView = TransferAlmostUsedFreeUserDialogScreenEvent,
                upgradeButtonPressed = TransferAlmostUsedFreeUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = TransferAlmostUsedFreeUserViewAllPlansButtonPressedEvent,
            )
        }

        QuotaWarningScenario.TransferAllUsed -> when {
            !isLoggedIn -> QuotaWarningEvents(
                screenView = TransferAllUsedNotLoggedInUserDialogScreenEvent,
                upgradeButtonPressed = TransferAllUsedNotLoggedInUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = TransferAllUsedNotLoggedInUserViewAllPlansButtonPressedEvent,
            )

            isProUser -> QuotaWarningEvents(
                screenView = TransferAllUsedProUserDialogScreenEvent,
                upgradeButtonPressed = TransferAllUsedProUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = TransferAllUsedProUserViewAllPlansButtonPressedEvent,
            )

            else -> QuotaWarningEvents(
                screenView = TransferAllUsedFreeUserDialogScreenEvent,
                upgradeButtonPressed = TransferAllUsedFreeUserUpgradeButtonPressedEvent,
                viewAllPlansButtonPressed = TransferAllUsedFreeUserViewAllPlansButtonPressedEvent,
            )
        }
    }
}
