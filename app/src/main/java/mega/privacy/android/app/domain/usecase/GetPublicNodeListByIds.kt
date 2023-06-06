package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class GetPublicNodeListByIds @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
) {
    suspend operator fun invoke(ids: List<Long>): List<MegaNode> =
        ids.mapNotNull {
            megaNodeRepository.getPublicNodeByHandle(it)
        }
}