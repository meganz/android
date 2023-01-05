package mega.privacy.android.app.smsVerification.model

import mega.privacy.android.domain.exception.SMSVerificationException

/**
 * SMSVerificationUIEvent
 */
sealed interface SMSVerificationUIEvent {

    /**
     * Failure
     */
    class Failure(
        /**
         * exception [SMSVerificationException]
         */
        val exception: SMSVerificationException,
    ) : SMSVerificationUIEvent

    /**
     * Progress
     */
    object Progress : SMSVerificationUIEvent

    /**
     * Success
     */
    object Success : SMSVerificationUIEvent

    /**
     * CallingCodesRetrieved
     */
    object CallingCodesRetrieved : SMSVerificationUIEvent

    /**
     * SMSCodeSent
     */
    object SMSCodeSent : SMSVerificationUIEvent
}
