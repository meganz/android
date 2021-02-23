package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import android.content.Intent
import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_COOKIE_SETTINGS_SAVED
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.*
import nz.mega.sdk.*
import java.util.*
import javax.inject.Inject

/**
 * Use Case to update cookie settings on SDK
 */
class UpdateCookieSettingsUseCase @Inject constructor(
    private val megaApi: MegaApiAndroid
) {

    /**
     * Save cookie settings to SDK
     *
     * @param cookies Set of cookies to be enabled
     * @return Observable with the operation result
     */
    fun update(cookies: Set<CookieType>?): Completable =
        Completable.create { emitter ->
            if (cookies.isNullOrEmpty()) {
                emitter.onError(IllegalArgumentException("Cookies are null or empty"))
                return@create
            }

            val bitSet = BitSet(CookieType.values().size).apply {
                this[CookieType.ESSENTIAL.value] = true // Essential cookies are always enabled
            }

            cookies.forEach { cookie ->
                bitSet[cookie.value] = true
            }

            val bitSetToDecimal = bitSet.toLongArray().first().toInt()

            megaApi.setCookieSettings(bitSetToDecimal, object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

                override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

                override fun onRequestFinish(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    if (emitter.isDisposed) return

                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            emitter.onComplete()
                        }
                        else -> {
                            emitter.onError(error.toThrowable())
                        }
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    logError(error.toThrowable().stackTraceToString())
                }
            })
        }.doOnComplete {
            val intent = Intent(BROADCAST_ACTION_COOKIE_SETTINGS_SAVED)
            MegaApplication.getInstance()?.sendBroadcast(intent)
        }

    /**
     * Save cookie settings with all the cookies enabled
     *
     * @return Observable with the operation result
     */
    fun acceptAll(): Completable =
        update(CookieType.values().toMutableSet())
}
