package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.COMPLETED_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Creates the intent to open transfers section
 */
class OpenTransfersSectionIntentMapper @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @ApplicationContext private val context: Context,
) {

    /**
     * Invoke
     * @param tab the tab to be selected
     */
    suspend operator fun invoke(
        tab: TransfersTab,
    ): Intent =
        if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(
                    EXTRA_TAB,
                    if (tab == TransfersTab.COMPLETED_TAB) COMPLETED_TAB_INDEX
                    else IN_PROGRESS_TAB_INDEX
                )
            }
        } else {
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_SHOW_TRANSFERS
                putExtra(ManagerActivity.TRANSFERS_TAB, tab)
            }
        }
}