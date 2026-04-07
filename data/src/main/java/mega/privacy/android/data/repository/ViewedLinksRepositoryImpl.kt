package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

class ViewedLinksRepositoryImpl @Inject constructor() : ViewedLinksRepository {
    override fun monitorLinks(): Flow<List<ViewedLink>> {
        TODO("Not yet implemented")
    }

    override suspend fun saveLink(viewedLink: ViewedLink) {
        TODO("Not yet implemented")
    }

    override suspend fun removeLnk(nodeHandle: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun clearLinks() {
        TODO("Not yet implemented")
    }
}