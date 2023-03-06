package mega.privacy.android.domain.usecase.filenode

import kotlinx.coroutines.flow.Flow

/**
 * Monitor account security upgrade
 */
fun interface MonitorSecurityUpgrade {
    operator fun invoke(): Flow<Boolean>
}
