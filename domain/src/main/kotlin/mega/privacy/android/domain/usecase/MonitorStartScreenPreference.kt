package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Monitor start screen preference
 */
fun interface MonitorStartScreenPreference {
    operator fun invoke(): Flow<StartScreen>
}