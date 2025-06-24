package mega.privacy.android.app.extensions

import android.content.Context
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.FAILED_TAB_INDEX
import mega.privacy.android.navigation.AppNavigator

/**
 * Utility function to open the transfers screen, selecting the appropriate tab and consuming the error status if corresponds
 */
@Deprecated("This should be handled in transfer widget view model, that will be created once TransfersSection feature flag is removed")
fun AppNavigator.openTransfersAndConsumeErrorStatus(
    context: Context,
    transfersManagementViewModel: TransfersManagementViewModel,
    defaultTab: Int = ACTIVE_TAB_INDEX,
) {
    openTransfers(
        context,
        if (transfersManagementViewModel.shouldCheckTransferError()) FAILED_TAB_INDEX else defaultTab
    )
}