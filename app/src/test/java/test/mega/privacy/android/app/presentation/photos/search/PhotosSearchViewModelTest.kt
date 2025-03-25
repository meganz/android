package test.mega.privacy.android.app.presentation.photos.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.photos.albums.AlbumTitleStringMapper
import mega.privacy.android.app.presentation.photos.albums.model.AlbumTitle
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.search.PhotosSearchViewModel
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.RetrievePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.SavePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
internal class PhotosSearchViewModelTest {
    private val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val albumTitleStringMapper: AlbumTitleStringMapper = mock()

    private val retrievePhotosRecentQueriesUseCase: RetrievePhotosRecentQueriesUseCase = mock()

    private val savePhotosRecentQueriesUseCase: SavePhotosRecentQueriesUseCase = mock()

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()

    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    private lateinit var photosSearchViewModel: PhotosSearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        photosSearchViewModel = PhotosSearchViewModel(
            defaultDispatcher = defaultDispatcher,
            albumTitleStringMapper = albumTitleStringMapper,
            retrievePhotosRecentQueriesUseCase = retrievePhotosRecentQueriesUseCase,
            savePhotosRecentQueriesUseCase = savePhotosRecentQueriesUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    @Test
    fun `test that data source populates properly`() = runTest {
        // given
        val albumsFlow = flowOf(
            listOf(
                UIAlbum(
                    id = Album.FavouriteAlbum,
                    title = AlbumTitle.StringTitle(""),
                    count = 0,
                    imageCount = 0,
                    videoCount = 0,
                    coverPhoto = null,
                    defaultCover = null,
                )
            )
        )

        val photosFlow = flowOf(
            listOf(
                Photo.Image(
                    id = 1L,
                    albumPhotoId = null,
                    parentId = 0L,
                    name = "",
                    isFavourite = false,
                    creationTime = LocalDateTime.now(),
                    modificationTime = LocalDateTime.now(),
                    thumbnailFilePath = null,
                    previewFilePath = null,
                    fileTypeInfo = StaticImageFileTypeInfo(
                        mimeType = "",
                        extension = "",
                    ),
                    size = 0L,
                    isTakenDown = false,
                    isSensitive = false,
                    isSensitiveInherited = false,
                )
            )
        )

        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())

        whenever(getFeatureFlagValueUseCase(any())).thenReturn(false)

        // when
        photosSearchViewModel.initialize(albumsFlow, photosFlow)
        advanceUntilIdle()

        // then
        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.albumsSource.size).isEqualTo(1)
            assertThat(state.photosSource.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that search works properly`() = runTest {
        // given
        val query = "sample"

        // when
        photosSearchViewModel.search(query)

        // then
        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.query).isEqualTo(query)
        }
    }
}
