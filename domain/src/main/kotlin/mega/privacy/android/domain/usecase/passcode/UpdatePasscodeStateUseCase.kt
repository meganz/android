package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Update passcode state use case
 *
 * @property passcodeRepository
 */
class UpdatePasscodeStateUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @param currentTime
     */
    suspend operator fun invoke(currentTime: Long) {
        if (passcodeRepository.monitorIsPasscodeEnabled().firstOrNull() != true) return

        when (val timeOut = passcodeRepository.monitorPasscodeTimeOut().firstOrNull()) {
            is PasscodeTimeout.Immediate -> {
                passcodeRepository.setLocked(true)
            }

            is PasscodeTimeout.TimeSpan -> {
                checkTimeSpan(currentTime, timeOut)
            }

            null -> return
        }
    }

    private suspend fun checkTimeSpan(
        currentTime: Long,
        timeOut: PasscodeTimeout.TimeSpan,
    ) {
        if (shouldLock(
                passcodeRepository.getLastPausedTime(),
                currentTime,
                timeOut
            )
        ) passcodeRepository.setLocked(
            true
        )
    }

    private fun shouldLock(
        lastPaused: Long?,
        currentTime: Long,
        timeOut: PasscodeTimeout.TimeSpan,
    ) = lastPaused == null || currentTime - lastPaused >= timeOut.milliseconds
}
