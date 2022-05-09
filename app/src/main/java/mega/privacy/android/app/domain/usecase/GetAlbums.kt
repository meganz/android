package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.Album

interface GetAlbums {

    operator fun invoke(): Flow<List<Album>>
}