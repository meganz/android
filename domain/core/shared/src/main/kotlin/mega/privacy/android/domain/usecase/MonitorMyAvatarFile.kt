package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * The use case interface to monitor my avatar file
 */
fun interface MonitorMyAvatarFile {
    /**
     * invoke
     */
    operator fun invoke(): Flow<File?>
}