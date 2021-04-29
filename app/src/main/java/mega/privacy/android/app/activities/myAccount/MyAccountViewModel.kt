package mega.privacy.android.app.activities.myAccount

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.activities.exportMK.ExportRecoveryKeyActivity
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.utils.Constants.*
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

    fun refresh(activity: Activity) {
        val intent = Intent(activity, LoginActivityLollipop::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        intent.action = ACTION_REFRESH

        activity.startActivityForResult(intent, REQUEST_CODE_REFRESH)
    }

    fun manageActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) {
            return
        }

        if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
            val app = MegaApplication.getInstance()

            app.askForAccountDetails()
            app.askForExtendedAccountDetails()
            LiveEventBus.get(EVENT_REFRESH).post(true)
        }
    }
}