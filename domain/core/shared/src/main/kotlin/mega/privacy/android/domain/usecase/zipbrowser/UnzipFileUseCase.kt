package mega.privacy.android.domain.usecase.zipbrowser

import mega.privacy.android.domain.repository.ZipBrowserRepository
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Unzip file use case
 */
class UnzipFileUseCase @Inject constructor(
    private val zipBrowserRepository: ZipBrowserRepository,
) {

    /**
     * Unzip file
     * @param  zipFile ZipFile
     * @param unzipRootPath unzip destination path
     * @return true is unzip succeed.
     */
    suspend operator fun invoke(zipFile: ZipFile?, unzipRootPath: String?) =
        if (zipFile != null && unzipRootPath != null) {
            zipBrowserRepository.unzipFile(zipFile, unzipRootPath)
        } else {
            false
        }
}