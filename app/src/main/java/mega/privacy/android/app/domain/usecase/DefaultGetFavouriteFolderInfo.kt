package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.repository.FavouritesRepository
import javax.inject.Inject

/**
 * The use case implementation class to get children nodes by node
 * @param repository FavouritesRepository
 */
class DefaultGetFavouriteFolderInfo @Inject constructor(private val repository: FavouritesRepository) :
    GetFavouriteFolderInfo {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(parentHandle: Long): Flow<FavouriteFolderInfo?> =
        flow {
            emit(repository.getChildren(parentHandle))
            emitAll(repository.monitorNodeChange().mapLatest {
                repository.getChildren(parentHandle)
            })
        }
}