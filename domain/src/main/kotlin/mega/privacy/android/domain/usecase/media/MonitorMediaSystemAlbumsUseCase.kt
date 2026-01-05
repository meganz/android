package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject

/**
 * Use case for retrieving predefined system albums from the photo repository.
 *
 * This use case builds a list of [MediaAlbum.System] objects representing system-defined media
 * categories. Each album includes a cover photo (if available) and uses the configured
 * [SystemAlbum] to determine which media items belong to that album.
 *
 * The album covers are determined by the first matching photo for each filter, if any exist.
 * This use case reactively combines photos, hidden items setting, and account details to
 * automatically update system albums when any of these dependencies change.
 *
 *  @property photosRepository Repository providing access to photo data.
 *  @property systemAlbums Set of configured system album types.
 *  @property defaultDispatcher Coroutine dispatcher used for background execution.
 *  @property monitorShowHiddenItemsUseCase Use case for monitoring hidden items setting.
 *  @property monitorAccountDetailUseCase Use case for monitoring account details.
 */
class MonitorMediaSystemAlbumsUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val systemAlbums: Set<@JvmSuppressWildcards SystemAlbum>,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) {
    /**
     * Retrieves a reactive stream of system-defined media albums.
     *
     * This method combines photos, hidden items setting, and account details into a single
     * reactive stream that automatically updates whenever any of the dependencies change.
     * This provides real-time updates for system albums based on current user settings
     * and account status.
     *
     * @return Flow of [List<MediaAlbum.System>] that emits updated albums whenever
     *         photos, hidden items setting, or account details change.
     */
    operator fun invoke() = combine(
        photosRepository.monitorPhotos(),
        monitorShowHiddenItemsUseCase(),
        monitorAccountDetailUseCase()
    ) { photos, showHiddenItems, accountDetail ->
        val isPaid = accountDetail.levelDetail?.accountType?.isPaid ?: false
        val isHiddenEnabled = (showHiddenItems && isPaid)

        systemAlbums.map { albumType ->
            val cover = photos
                .filter { photo ->
                    isHiddenEnabled || (!photo.isSensitive && !photo.isSensitiveInherited)
                }
                .firstOrNull { photo ->
                    albumType.filter(photo)
                }
            MediaAlbum.System(id = albumType, cover = cover)
        }
    }.flowOn(defaultDispatcher)
}