package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import javax.inject.Inject

/**
 * Unlock passcode use case
 */
class UnlockPasscodeUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @param request
     */
    suspend operator fun invoke(request: UnlockPasscodeRequest) {
        TODO()
    }
}
