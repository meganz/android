package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that retrieves User information on the list of backed up Devices, represented as a [Map].
 * Each [Map] entry represents a Key-Value Pair of a Device ID and Device Name, respectively
 */
class GetDeviceIdAndNameMapUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * Invocation function
     *
     * @return A [Map] whose Key-Value Pair consists of the Device ID and Device Name
     */
    suspend operator fun invoke(): Map<String, String> = backupRepository.getDeviceIdAndNameMap()
}