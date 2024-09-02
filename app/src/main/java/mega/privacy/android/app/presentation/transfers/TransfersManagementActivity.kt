package mega.privacy.android.app.presentation.transfers

import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Activity for showing concrete UI items related to transfers management.
 */
@AndroidEntryPoint
open class TransfersManagementActivity : PasscodeActivity() {

    protected val transfersManagementViewModel: TransfersManagementViewModel by viewModels()
    protected val transfersViewModel: TransfersViewModel by viewModels()

    /**
     * [MegaNavigator]
     */
    @Inject
    lateinit var navigator: MegaNavigator
}
