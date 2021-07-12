package mega.privacy.android.app.myAccount.usecase

import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_NAME_UPDATED
import mega.privacy.android.app.di.MegaApi
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

    fun updateFirstName(firstName: String): Single<Boolean> =
        Single.create { emitter ->
            megaApi.setUserAttribute(
                USER_ATTR_FIRSTNAME,
                firstName,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    updateFirstName(request.text, error)
                    finishAction(error.errorCode == API_OK, emitter)
                })
            )
        }

    private fun updateFirstName(lastName: String, error: MegaError) {
        if (error.errorCode == API_OK) {
            myAccountInfo.setFirstNameText(lastName)
        }
    }

    fun updateLastName(firstName: String): Single<Boolean> =
        Single.create { emitter ->
            megaApi.setUserAttribute(
                USER_ATTR_LASTNAME,
                firstName,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    updateLastName(request.text, error)
                    finishAction(error.errorCode == API_OK, emitter)
                })
            )
        }

    private fun updateLastName(lastName: String, error: MegaError) {
        if (error.errorCode == API_OK) {
            myAccountInfo.setLastNameText(lastName)
        }
    }

    private fun finishAction(success: Boolean, emitter: SingleEmitter<Boolean>) {
        if (success) {
            LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean::class.java).post(true)
        }

        emitter.onSuccess(success)
    }

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

    fun updateEmail(email: String): Single<MegaError> = Single.create { emitter ->
        megaApi.changeEmail(
            email,
            OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                emitter.onSuccess(error)
            })
        )
    }
}