package mega.privacy.android.app.psa

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.Event
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

    private val _psa = MutableLiveData<Event<Psa>>()
    val psa: LiveData<Event<Psa>> = _psa
    var psaState = PSA_STATE_IDLE

    fun checkPsa() {
        if (System.currentTimeMillis() - lastGetPsaTime < GET_PSA_MIN_INTERVAL_MS
            || psaState == PSA_STATE_DISPLAYING
        ) {
            return
        }

        val event = psa.value
        if (psaState == PSA_STATE_PENDING_DISPLAY && event != null) {
            _psa.value = Event(event.peekContent())
            return
        }

        megaApi.getPSA(object : BaseListener(getApplication()) {
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
                    psaState = PSA_STATE_PENDING_DISPLAY
                    _psa.value = Event(
                        Psa(
                            request.number.toInt(), request.name, request.text, request.file,
                            request.password, request.link, request.email
                        )
                    )
                }
            }
        })
    }

    fun dismissPsa(id: Int) {
        psaState = PSA_STATE_IDLE
        megaApi.setPSA(id.toInt())
    }

    companion object {
        const val LAST_GET_PSA_SP = "LAST_GET_PSA_TIME_SP"
        const val LAST_GET_PSA_KEY = "LAST_GET_PSA_TIME_KEY"

        const val GET_PSA_MIN_INTERVAL_MS = 3600_000

        const val PSA_STATE_IDLE = 1
        const val PSA_STATE_PENDING_DISPLAY = 2
        const val PSA_STATE_DISPLAYING = 3
    }
}

data class Psa(
    val id: Int,
    val title: String,
    val text: String,
    val imageUrl: String?,
    val positiveText: String?,
    val positiveLink: String?,
    val url: String?
)
