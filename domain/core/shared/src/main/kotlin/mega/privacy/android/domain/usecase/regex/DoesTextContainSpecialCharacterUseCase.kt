package mega.android.authentication.domain.usecase.regex

import javax.inject.Inject

/**
 * Use case to check if a string contains special characters
 */
class DoesTextContainSpecialCharacterUseCase @Inject constructor() {

    /**
     * Invoke
     * @param text as string to check
     */
    operator fun invoke(text: String?): Boolean = text?.contains(Regex("(?=.*[-+_!@#\$%^&*.,?])")) == true
}