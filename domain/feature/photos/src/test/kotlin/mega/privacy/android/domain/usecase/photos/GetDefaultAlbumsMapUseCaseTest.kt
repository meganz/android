package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetDefaultAlbumsMapUseCaseTest {
    private lateinit var underTest: GetDefaultAlbumsMapUseCase

    private val favouritePredicate = mock<PhotoPredicate>()
    private val gifPredicate = mock<PhotoPredicate>()
    private val rawPredicate = mock<PhotoPredicate>()

    private val filterFavouriteUseCase = mock<FilterFavouriteUseCase>()

    private val filterGIFUseCase = mock<FilterGIFUseCase>()

    private val filterRAWUseCase = mock<FilterRAWUseCase>()

    @Before
    fun setUp() {
        whenever(filterFavouriteUseCase()).thenAnswer { favouritePredicate }
        whenever(filterGIFUseCase()).thenAnswer { gifPredicate }
        whenever(filterRAWUseCase()).thenAnswer { rawPredicate }

        underTest = GetDefaultAlbumsMapUseCase(
            filterFavouriteUseCase = filterFavouriteUseCase,
            filterGIFUseCase = filterGIFUseCase,
            filterRAWUseCase = filterRAWUseCase,
        )
    }

    @Test
    fun `test that correct values are returned`() {
        val expected = linkedMapOf(
            Album.FavouriteAlbum to favouritePredicate,
            Album.GifAlbum to gifPredicate,
            Album.RawAlbum to rawPredicate,
        )

        assertThat(underTest()).containsExactlyEntriesIn(expected)
    }
}
