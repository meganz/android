package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Use case for monitoring the names of the user-created albums.
 *
 * Emits the current list of user album names on start and re-emits whenever the user's album sets
 * change (create / rename / delete). Unlike [MonitorMediaAlbumsUseCase], it does not resolve cover
 * photos, making it a lightweight source when only the names are needed.
 *
 * @property albumRepository Repository providing access to user-created album data.
 * @property defaultDispatcher Coroutine dispatcher used for background operations.
 *
 * @return A [Flow] emitting the list of user album names whenever updates occur.
 */
class MonitorUserAlbumNamesUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<List<String>> =
        albumRepository.monitorUserSetsUpdate()
            .map { getUserAlbumNames() }
            .onStart { emit(getUserAlbumNames()) }

    private suspend fun getUserAlbumNames(): List<String> =
        withContext(defaultDispatcher) {
            albumRepository.getAllUserSets().map { it.name }
        }
}
