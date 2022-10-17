package mega.privacy.android.app.myAccount.usecase

import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_NAME_UPDATED
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME
import nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class UpdateMyUserAttributesUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val myAccountInfo: MyAccountInfo
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
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    updateFirstName(request.text, error)

                    val success = error.errorCode == API_OK
                    finishAction(success)
                    emitter.onSuccess(success)
                })
            )
        }

    /**
     * Updates the first name on MyAccountInfo.
     *
     * @param firstName New first name.
     * @param error     Error of the request.
     */
    private fun updateFirstName(firstName: String, error: MegaError) {
        if (error.errorCode == API_OK) {
            myAccountInfo.setFirstNameText(firstName)
        }
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
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    updateLastName(request.text, error)

                    val success = error.errorCode == API_OK
                    finishAction(success)
                    emitter.onSuccess(success)
                })
            )
        }

    /**
     * Updates the last name on MyAccountInfo.
     *
     * @param lastName New first name.
     * @param error    Error of the request.
     */
    private fun updateLastName(lastName: String, error: MegaError) {
        if (error.errorCode == API_OK) {
            myAccountInfo.setLastNameText(lastName)
        }
    }

    /**
     * Finishes the request action (change first or last name) by launching an update event if success.
     *
     * @param success True if the action finished successfully, false otherwise.
     */
    private fun finishAction(success: Boolean) {
        if (success) {
            LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean::class.java).post(true)
        }
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

                    var someChange = false

                    if (request.paramType == USER_ATTR_FIRSTNAME) {
                        updateFirstName(request.text, error)
                        if (error.errorCode != API_OK) success = false
                        else someChange = true
                    } else if (request.paramType == USER_ATTR_LASTNAME) {
                        updateLastName(request.text, error)
                        if (error.errorCode != API_OK) success = false
                        else someChange = true
                    }

                    if (requestFinished == 0) {
                        if (someChange) {
                            LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean::class.java)
                                .post(true)
                        }

                        emitter.onSuccess(success)
                    }
                })

            megaApi.setUserAttribute(USER_ATTR_FIRSTNAME, firstName, listener)
            megaApi.setUserAttribute(USER_ATTR_LASTNAME, lastName, listener)
        }
    }

    /**
     * Launches a request to set a new email for the current account.
     *
     * @return Single<Boolean> True if action finished with success, false otherwise.
     */
    fun updateEmail(email: String): Single<MegaError> = Single.create { emitter ->
        megaApi.changeEmail(
            email,
            OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                emitter.onSuccess(error)
            })
        )
    }
}