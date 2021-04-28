package mega.privacy.android.app.activities.myAccount

import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.activities.exportMK.ExportRecoveryKeyActivity
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.utils.Constants.MY_ACCOUNT_FRAGMENT
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError

class MyAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private val killSessions: MutableLiveData<MegaError> = MutableLiveData()

    private var fragment = MY_ACCOUNT_FRAGMENT

    fun onKillSessionsFinished(): LiveData<MegaError> = killSessions

    fun setFragment(fragment: Int) {
        this.fragment = fragment
    }

    fun isMyAccountFragment(): Boolean = fragment == MY_ACCOUNT_FRAGMENT

    fun thereIsNoSubscription(): Boolean = myAccountInfo.numberOfSubscriptions <= 0

    fun killSessions() {
        megaApi.killSession(INVALID_HANDLE, OptionalMegaRequestListenerInterface(
            onRequestFinish = { _, error ->
                if (error.errorCode == MegaError.API_OK) {
                    logDebug("Success kill sessions")
                } else {
                    logError("Error when killing sessions: " + error.errorString)
                }

                killSessions.value = error
            }
        ))
    }

    fun changePassword(context: Context) {
        context.startActivity(Intent(context, ChangePasswordActivityLollipop::class.java))
    }

    fun exportMK(context: Context) {
        context.startActivity(Intent(context, ExportRecoveryKeyActivity::class.java))
    }
}