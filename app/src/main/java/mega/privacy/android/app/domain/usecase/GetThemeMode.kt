package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.ThemeMode


/**
 * Get theme mode preference
 */
fun interface GetThemeMode {

    /**
     * Invoke
     *
     * @return Current Theme mode and any subsequent changes as a flow
     */
    operator fun invoke(): Flow<ThemeMode>
}