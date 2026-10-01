package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Checks if request phone number was already shown or not from Data Store,The default value is set to false
 */
class IsRequestPhoneNumberShownUseCase @Inject constructor(private val repository: VerificationRepository) {


    /**
     * invoke
     * @return [Boolean] if request phone number was already shown or not from Data Store,The default value is set to false
     */
    suspend operator fun invoke() = repository.isRequestPhoneNumberShown()

}