package mega.privacy.android.feature.photos.provider.implementation

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.usecase.media.MonitorMediaSystemAlbumsUseCase
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import javax.inject.Inject

class SystemAlbumsDataProvider @Inject constructor(
    private val monitorMediaSystemAlbumsUseCase: MonitorMediaSystemAlbumsUseCase,
) : AlbumsDataProvider {
    override val order: Int = 1

    override fun monitorAlbums(): Flow<List<MediaAlbum>> = monitorMediaSystemAlbumsUseCase()
}