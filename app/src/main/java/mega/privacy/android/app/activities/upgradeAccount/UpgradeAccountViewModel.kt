package mega.privacy.android.app.activities.upgradeAccount

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.globalmanagement.MyAccountInfo

class UpgradeAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo
) : BaseRxViewModel() {

}