package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import javax.inject.Inject

/**
 * Creates the intent to open transfers section
 */
class OpenTransfersSectionIntentMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Invoke
     * @param tab the tab to be selected
     */
    operator fun invoke(
        tab: TransfersTab,
    ): Intent = Intent(context, TransfersActivity::class.java).apply {
        putExtra(
            EXTRA_TAB,
            if (tab == TransfersTab.COMPLETED_TAB) COMPLETED_TAB_INDEX
            else ACTIVE_TAB_INDEX
        )
    }
}