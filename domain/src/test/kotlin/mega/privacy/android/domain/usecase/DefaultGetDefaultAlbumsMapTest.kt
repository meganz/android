package mega.privacy.android.domain.usecase

import AlbumEntity
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DefaultGetDefaultAlbumsMapTest {
    private lateinit var underTest: GetDefaultAlbumsMap

    private val favouritePredicate = mock<PhotoPredicate>()
    private val gifPredicate = mock<PhotoPredicate>()
    private val rawPredicate = mock<PhotoPredicate>()

    private val filterFavourite = mock<FilterFavourite>()

    private val filterGIF = mock<FilterGIF>()

    private val filterRAW = mock<FilterRAW>()

    @Before
    fun setUp() {
        whenever(filterFavourite()).thenAnswer { favouritePredicate }
        whenever(filterGIF()).thenAnswer { gifPredicate }
        whenever(filterRAW()).thenAnswer { rawPredicate }

        underTest = DefaultGetDefaultAlbumsMap(
            filterFavourite = filterFavourite,
            filterGIF = filterGIF,
            filterRAW = filterRAW,
        )
    }

    @Test
    fun `test that correct values are returned`() {
        val expected = linkedMapOf(
            AlbumEntity.FavouriteAlbum to favouritePredicate,
            AlbumEntity.GifAlbum to gifPredicate,
            AlbumEntity.RawAlbum to rawPredicate,
        )

        assertThat(underTest()).containsExactlyEntriesIn(expected)
    }
}
