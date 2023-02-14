package mega.privacy.android.app.smsVerification.model.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.SMSVerificationException
import javax.inject.Inject

class SmsVerificationTextErrorMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsVerificationTextErrorMapper {
    override fun invoke(error: Throwable): String {
        return when (error) {
            is SMSVerificationException.AlreadyExists -> context.getString(R.string.verify_account_error_phone_number_register)
            is SMSVerificationException.AlreadyVerified -> context.getString(R.string.verify_account_error_code_verified)
            is SMSVerificationException.LimitReached -> context.getString(R.string.verify_account_error_reach_limit)
            is SMSVerificationException.VerificationCodeDoesNotMatch -> context.getString(R.string.verify_account_error_wrong_code)
            else -> context.getString(R.string.verify_account_error_invalid_code)
        }
    }
}
