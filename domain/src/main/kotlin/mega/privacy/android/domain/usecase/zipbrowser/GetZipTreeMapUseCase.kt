package mega.privacy.android.domain.usecase.zipbrowser

import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.repository.ZipBrowserRepository
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * The use case for getting zip tree map
 */
class GetZipTreeMapUseCase @Inject constructor(
    private val zipBrowserRepository: ZipBrowserRepository,
) {

    /**
     * Getting zip tree map
     *
     * @param zipFile ZipFile
     */
    suspend operator fun invoke(zipFile: ZipFile?): Map<String, ZipTreeNode> =
        zipBrowserRepository.getZipNodeTree(zipFile)
}