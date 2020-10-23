package mega.privacy.android.app.psa

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class PsaViewModel(
    private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {
    private val preference = getApplication<MegaApplication>().getSharedPreferences(
        LAST_GET_PSA_SP, Context.MODE_PRIVATE
    )
    private var lastGetPsaTime = preference.getLong(LAST_GET_PSA_KEY, 0)

    private val _psa = MutableLiveData<Psa?>()
    val psa: LiveData<Psa?> = _psa

    fun checkPsa() {
        if (System.currentTimeMillis() - lastGetPsaTime < GET_PSA_MIN_INTERVAL_MS) {
            return
        }

        if (_psa.value != null) {
            _psa.notifyObserver()
            return
        }

        megaApi.getPSAWithUrl(object : BaseListener(getApplication()) {
            override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
                super.onRequestFinish(api, request, e)

                if (request == null || e == null) {
                    return
                }

                if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_ENOENT) {
                    lastGetPsaTime = System.currentTimeMillis()
                    preference.edit()
                        .putLong(LAST_GET_PSA_KEY, lastGetPsaTime)
                        .apply()
                }

                if (e.errorCode == MegaError.API_OK) {
                    _psa.value = Psa(
                        request.number.toInt(), request.name, request.text, request.file,
                        request.password, request.link, request.email
                    )
                }
            }
        })
    }

    fun dismissPsa(id: Int) {
        megaApi.setPSA(id)
        _psa.value = null
    }

    companion object {
        const val LAST_GET_PSA_SP = "LAST_GET_PSA_TIME_SP"
        const val LAST_GET_PSA_KEY = "LAST_GET_PSA_TIME_KEY"

        const val GET_PSA_MIN_INTERVAL_MS = 3600_000
    }
}
