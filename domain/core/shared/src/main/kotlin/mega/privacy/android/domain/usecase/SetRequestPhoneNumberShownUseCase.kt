package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Sets the status of showing request the phone number
 */
class SetRequestPhoneNumberShownUseCase @Inject constructor(private val repository: VerificationRepository) {

    /**
     * invoke
     * @param isShown status for RequestPhoneNumber
     */
    suspend operator fun invoke(isShown: Boolean) = repository.setRequestPhoneNumberShown(isShown)

}