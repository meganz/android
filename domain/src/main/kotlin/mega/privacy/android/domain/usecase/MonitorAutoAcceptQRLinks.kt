package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor auto accept QR links setting use case
 */
fun interface MonitorAutoAcceptQRLinks {
    /**
     * Invoke
     *
     * @return flow of changes to the setting
     */
    operator fun invoke(): Flow<Boolean>
}
