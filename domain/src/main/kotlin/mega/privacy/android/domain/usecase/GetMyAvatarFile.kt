package mega.privacy.android.domain.usecase

import java.io.File

/**
 * The use case interface to get my avatar file
 */
fun interface GetMyAvatarFile {
    /**
     * invoke
     */
    suspend operator fun invoke(): File?
}