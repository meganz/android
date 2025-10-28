package mega.privacy.android.feature.photos.provider.implementation

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.usecase.media.MonitorMediaAlbumsUseCase
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import javax.inject.Inject

class UserAlbumsDataProvider @Inject constructor(
    private val monitorMediaAlbumsUseCase: MonitorMediaAlbumsUseCase,
) : AlbumsDataProvider {
    override val order: Int = 2

    override fun monitorAlbums(): Flow<List<MediaAlbum>> = monitorMediaAlbumsUseCase()
}