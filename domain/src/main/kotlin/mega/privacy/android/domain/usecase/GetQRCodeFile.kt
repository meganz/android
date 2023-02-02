package mega.privacy.android.domain.usecase

import java.io.File

/**
 * Use case to get the file handle of QR code file
 */
fun interface GetQRCodeFile {

    /**
     * invoke method
     *
     * @return file handle. A non-null handle is returned even if file does not exist.
     */
    suspend operator fun invoke(): File?
}