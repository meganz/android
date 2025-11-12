package mega.privacy.android.feature.photos.presentation.albums.content

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.GifSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.RawSystemAlbum
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class AlbumContentUtilTest {

    @Test
    fun `test that user album is converted to album content nav key correctly`() {
        val userAlbum = MediaAlbum.User(
            id = AlbumId(123L),
            title = "Test Album",
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
            cover = null
        )

        val result = userAlbum.toAlbumContentNavKey()

        assertThat(result).isEqualTo(
            AlbumContentNavKey(
                id = 123L,
                type = "custom"
            )
        )
    }

    @Test
    fun `test that raw system album is converted to album content nav key correctly`() {
        val rawSystemAlbum = MediaAlbum.System(
            id = mock<RawSystemAlbum>(),
            cover = null
        )

        val result = rawSystemAlbum.toAlbumContentNavKey()

        assertThat(result).isEqualTo(
            AlbumContentNavKey(
                id = null,
                type = "raw"
            )
        )
    }

    @Test
    fun `test that gif system album is converted to album content nav key correctly`() {
        val gifSystemAlbum = MediaAlbum.System(
            id = mock<GifSystemAlbum>(),
            cover = null
        )

        val result = gifSystemAlbum.toAlbumContentNavKey()

        assertThat(result).isEqualTo(
            AlbumContentNavKey(
                id = null,
                type = "gif"
            )
        )
    }

    @Test
    fun `test that favourite system album is converted to album content nav key correctly`() {
        val favouriteSystemAlbum = MediaAlbum.System(
            id = mock<FavouriteSystemAlbum>(),
            cover = null
        )

        val result = favouriteSystemAlbum.toAlbumContentNavKey()

        assertThat(result).isEqualTo(
            AlbumContentNavKey(
                id = null,
                type = "favourite"
            )
        )
    }

    @Test
    fun `test that unknown system album is converted to album content nav key with null type`() {
        val unknownSystemAlbum = MediaAlbum.System(
            id = mock<SystemAlbum>(),
            cover = null
        )

        val result = unknownSystemAlbum.toAlbumContentNavKey()

        assertThat(result).isEqualTo(
            AlbumContentNavKey(
                id = null,
                type = null
            )
        )
    }
}

