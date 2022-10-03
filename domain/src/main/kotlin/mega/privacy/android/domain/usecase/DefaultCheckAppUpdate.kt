package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FirstVersionResult
import mega.privacy.android.domain.entity.NewVersionResult
import mega.privacy.android.domain.entity.SameVersionResult
import mega.privacy.android.domain.entity.VersionUpdateCheckResult
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Default implementation of [CheckAppUpdate]
 * Default check app update
 *
 */
class DefaultCheckAppUpdate @Inject constructor(
    private val repository: EnvironmentRepository,
) : CheckAppUpdate {
    override suspend fun invoke(): VersionUpdateCheckResult {
        val oldVersionCode = repository.getLastSavedVersionCode()
        val newVersionCode = repository.getInstalledVersionCode()
        return when {
            oldVersionCode == 0 -> FirstVersionResult(currentVersionCode = newVersionCode)
            oldVersionCode < newVersionCode -> NewVersionResult(oldVersionCode = oldVersionCode,
                newVersionCode = newVersionCode)
            else -> SameVersionResult(currentVersionCode = newVersionCode)
        }
    }
}