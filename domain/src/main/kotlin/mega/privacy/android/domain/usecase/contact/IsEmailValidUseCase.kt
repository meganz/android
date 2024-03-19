package mega.privacy.android.domain.usecase.contact

import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Use case to check the validity of an email
 */
class IsEmailValidUseCase @Inject constructor() {

    /**
     * Invocation method to check email's validity
     */
    operator fun invoke(email: String): Boolean =
        email.isNotBlank() && email.matches(EMAIL_ADDRESS_REGEX.toRegex())

    companion object {
        private val EMAIL_ADDRESS_REGEX = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\&\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
    }
}
