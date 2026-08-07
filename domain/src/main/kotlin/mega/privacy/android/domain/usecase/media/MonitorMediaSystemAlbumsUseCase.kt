package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject

/**
 * Use case for retrieving predefined system albums.
 *
 * This use case builds a list of [MediaAlbum.System] objects representing system-defined media
 * categories. Each album includes a cover node (if available) and uses its configured
 * [SystemAlbum.mediaTimelineFilter] to determine which media items belong to it.
 *
 * The cover is the first matching node for each album's filter, fetched directly through
 * [ListMediaNodesByOffsetUseCase] (one node per album) rather than filtering the full photo set in
 * memory. This use case reactively combines media node updates, the hidden-items setting and account
 * details, so system albums update automatically when any of these dependencies change.
 *
 * @property photosRepository Repository providing the media node update stream used as a trigger.
 * @property systemAlbums Set of configured system album types.
 * @property listMediaNodesByOffsetUseCase Use case fetching the album cover node for a given filter.
 * @property defaultDispatcher Coroutine dispatcher used for background execution.
 * @property monitorShowHiddenItemsUseCase Use case for monitoring hidden items setting.
 * @property monitorAccountDetailUseCase Use case for monitoring account details.
 */
class MonitorMediaSystemAlbumsUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val systemAlbums: Set<@JvmSuppressWildcards SystemAlbum>,
    private val listMediaNodesByOffsetUseCase: ListMediaNodesByOffsetUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) {
    /**
     * Retrieves a reactive stream of system-defined media albums.
     *
     * @return Flow of [List<MediaAlbum.System>] that emits updated albums whenever media nodes,
     *         the hidden items setting, or account details change.
     */
    operator fun invoke() = combine(
        photosRepository.monitorMediaTypedNodes,
        monitorShowHiddenItemsUseCase(),
        monitorAccountDetailUseCase(),
    ) { _, showHiddenItems, accountDetail ->
        val isPaid = accountDetail.levelDetail?.accountType?.isPaid ?: false
        val sensitivity = if (isPaid && !showHiddenItems) {
            Sensitivity.HideSensitive
        } else {
            Sensitivity.ShowAll
        }

        systemAlbums
            .mapNotNull map@{ systemAlbum ->
                val cover = listMediaNodesByOffsetUseCase(
                    filter = systemAlbum.mediaTimelineFilter.copy(sensitivity = sensitivity),
                    section = null,
                    order = SortOrder.ORDER_MODIFICATION_DESC,
                    maxElements = 1,
                    offset = 0,
                ).firstOrNull()

                if (systemAlbum.hideWhenEmpty && cover == null) {
                    return@map null
                }

                MediaAlbum.System(id = systemAlbum, cover = cover)
            }
    }.flowOn(defaultDispatcher)
}
