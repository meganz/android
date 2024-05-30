package mega.privacy.android.domain.usecase.zipbrowser

import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import javax.inject.Inject

/**
 * The use case for getting zip tree map
 */
class GetZipTreeMapUseCase @Inject constructor() {

    /**
     * Getting zip tree map
     *
     * @param zipFullPath zip file full path
     */
    operator fun invoke(zipFullPath: String): Map<String, ZipTreeNode> = emptyMap()
}