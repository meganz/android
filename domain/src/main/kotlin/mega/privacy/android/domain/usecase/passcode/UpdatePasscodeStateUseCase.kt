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
    suspend operator fun invoke(
        currentTime: Long,
        orientation: Int,
    ) {
        if (passcodeRepository.monitorIsPasscodeEnabled().firstOrNull() != true) return
        val lastOrientation = passcodeRepository.monitorLastOrientation().firstOrNull() ?: orientation

        when (val timeOut = passcodeRepository.monitorPasscodeTimeOut().firstOrNull()) {
            PasscodeTimeout.Immediate -> {
                if (lastOrientation != orientation) checkTimeSpan(
                    currentTime,
                    ROTATION_GRACE_MILLISECONDS
                ) else passcodeRepository.setLocked(true)
            }

            is PasscodeTimeout.TimeSpan -> {
                checkTimeSpan(currentTime, timeOut.milliseconds)
            }

            null -> return
        }

        passcodeRepository.setLastPausedTime(null)
    }

    private suspend fun checkTimeSpan(
        currentTime: Long,
        timeOutMilliseconds: Long,
    ) {
        if (shouldLock(
                passcodeRepository.getLastPausedTime(),
                currentTime,
                timeOutMilliseconds
            )
        ) passcodeRepository.setLocked(
            true
        )
    }

    private fun shouldLock(
        lastPaused: Long?,
        currentTime: Long,
        timeOutMilliseconds: Long,
    ) = lastPaused != null && currentTime - lastPaused >= timeOutMilliseconds

    companion object {
        internal const val ROTATION_GRACE_MILLISECONDS: Long = 700
    }
}
