package mega.privacy.android.app.activities.myAccount

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.Constants.MY_ACCOUNT_FRAGMENT
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

class MyAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private var fragment = MY_ACCOUNT_FRAGMENT

    fun isMyAccountFragment(): Boolean = fragment == MY_ACCOUNT_FRAGMENT

    fun thereIsNoSubscription(): Boolean = myAccountInfo.numberOfSubscriptions <= 0

    fun killSessions() {
        megaApi.killSession(INVALID_HANDLE)
    }
}