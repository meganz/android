package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.exception.security.NoPasscodeTypeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Get passcode type use case
 *
 * @property passcodeRepository
 */
class GetPasscodeTypeUseCase @Inject constructor(private val passcodeRepository: PasscodeRepository) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() =
        passcodeRepository.monitorPasscodeType()
            .firstOrNull() ?: throw NoPasscodeTypeSetException()

}
