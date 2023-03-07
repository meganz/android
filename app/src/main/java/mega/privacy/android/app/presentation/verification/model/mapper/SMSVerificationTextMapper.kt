package mega.privacy.android.app.presentation.verification.model.mapper

import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState

/**
 * Sms verification text mapper
 */
fun interface SMSVerificationTextMapper {
    /**
     * Invoke
     *
     * @param state [SMSVerificationUIState]
     * @return mapped state [SMSVerificationUIState]
     */
    operator fun invoke(state: SMSVerificationUIState): SMSVerificationUIState
}
