package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.Flow

/**
 * Monitor account security upgrade in app
 */
fun interface MonitorSecurityUpgradeInApp {
    operator fun invoke(): Flow<Boolean>
}
