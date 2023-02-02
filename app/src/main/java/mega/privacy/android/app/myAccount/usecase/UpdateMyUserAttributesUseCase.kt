package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME
import nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class UpdateMyUserAttributesUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    /**
     * Launches a request to set a new first name for the current account.
     *
     * @return Single<Boolean> True if action finished with success, false otherwise.
     */
    fun updateFirstName(firstName: String): Single<Boolean> =
        Single.create { emitter ->
            megaApi.setUserAttribute(
                USER_ATTR_FIRSTNAME,
                firstName,
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    emitter.onSuccess(error.errorCode == API_OK)
                })
            )
        }

    /**
     * Launches a request to set a new last name for the current account.
     *
     * @return Single<Boolean> True if action finished with success, false otherwise.
     */
    fun updateLastName(firstName: String): Single<Boolean> =
        Single.create { emitter ->
            megaApi.setUserAttribute(
                USER_ATTR_LASTNAME,
                firstName,
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    emitter.onSuccess(error.errorCode == API_OK)
                })
            )
        }

    /**
     * Launches a request to set a new first name and a new last name for the current account.
     *
     * @return Single<Boolean> True if action finished with success, false otherwise.
     */
    fun updateFirstAndLastName(
        firstName: String,
        lastName: String
    ): Single<Boolean> {
        var requestFinished = 2
        var success = true

        return Single.create { emitter ->
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    requestFinished--

                    if (request.paramType == USER_ATTR_FIRSTNAME) {
                        if (error.errorCode != API_OK) success = false
                    } else if (request.paramType == USER_ATTR_LASTNAME) {
                        if (error.errorCode != API_OK) success = false
                    }

                    if (requestFinished == 0) {
                        emitter.onSuccess(success)
                    }
                })

            megaApi.setUserAttribute(USER_ATTR_FIRSTNAME, firstName, listener)
            megaApi.setUserAttribute(USER_ATTR_LASTNAME, lastName, listener)
        }
    }
}