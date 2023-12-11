package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.rxjava


import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import java.util.BitSet
import javax.inject.Inject

/**
 * Use Case to get cookie settings from SDK
 */
@Deprecated("Use GetCookieSettingsUseCase instead")
class GetCookieSettingsUseCaseRx @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    /**
     * Get current cookie settings from SDK
     *
     * @return Observable with a set of enabled cookies
     */
    fun get(): Single<Set<CookieType>> =
        Single.create { emitter ->
            megaApi.getCookieSettings(OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            val result = mutableSetOf<CookieType>()

                            val bitSet = BitSet.valueOf(longArrayOf(request.numDetails.toLong()))
                            for (i in 0..bitSet.length()) {
                                if (bitSet[i]) {
                                    result.add(CookieType.valueOf(i))
                                }
                            }

                            emitter.onSuccess(result)
                        }

                        MegaError.API_ENOENT -> {
                            emitter.onSuccess(emptySet()) // Cookie Settings has not been set before
                        }

                        else -> {
                            emitter.onError(error.toThrowable())
                        }
                    }
                }
            ))
        }

    /**
     * Check if the cookie dialog should be shown
     *
     * @return Observable with the boolean flag
     */
    fun shouldShowDialog(): Single<Boolean> =
        get().map { it.isNullOrEmpty() }
}
