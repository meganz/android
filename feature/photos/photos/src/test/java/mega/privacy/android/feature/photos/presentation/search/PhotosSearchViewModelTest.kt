package mega.privacy.android.feature.photos.presentation.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosResult
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.RetrievePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.SavePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumTitleStringMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.feature.photos.provider.PhotosCache
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
internal class PhotosSearchViewModelTest {
    private val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val albumTitleStringMapper: AlbumTitleStringMapper = mock()

    private val retrievePhotosRecentQueriesUseCase: RetrievePhotosRecentQueriesUseCase = mock()

    private val savePhotosRecentQueriesUseCase: SavePhotosRecentQueriesUseCase = mock()

    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()

    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    private val albumsDataProvider: AlbumsDataProvider = mock()

    private val albumsProvider: Lazy<Set<AlbumsDataProvider>> = Lazy { setOf(albumsDataProvider) }

    private val albumUiStateMapper: AlbumUiStateMapper = mock()

    private val monitorTimelinePhotosUseCase: MonitorTimelinePhotosUseCase = mock()

    private val lazyMonitorTimelinePhotosUseCase: Lazy<MonitorTimelinePhotosUseCase> =
        Lazy { monitorTimelinePhotosUseCase }

    private lateinit var photosSearchViewModel: PhotosSearchViewModel

    private val accountLevelDetail = mock<AccountLevelDetail> {
        on { accountType }.thenReturn(AccountType.PRO_III)
    }
    private val accountDetail = mock<AccountDetail> {
        on { levelDetail }.thenReturn(accountLevelDetail)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // Reset PhotosCache before each test to prevent interference from previous tests
        PhotosCache.updatePhotos(emptyList())
        PhotosCache.updateAlbums(emptyList())

        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(flowOf(false))
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(flowOf(accountDetail))
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
    }

    @After
    fun tearDown() {
        PhotosCache.updatePhotos(emptyList())
        PhotosCache.updateAlbums(emptyList())
    }

    private fun initViewModel() {
        photosSearchViewModel = PhotosSearchViewModel(
            defaultDispatcher = defaultDispatcher,
            albumTitleStringMapper = albumTitleStringMapper,
            retrievePhotosRecentQueriesUseCase = retrievePhotosRecentQueriesUseCase,
            savePhotosRecentQueriesUseCase = savePhotosRecentQueriesUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            albumsProvider = albumsProvider,
            albumUiStateMapper = albumUiStateMapper,
            monitorTimelinePhotosUseCase = lazyMonitorTimelinePhotosUseCase,
            context = mock(),
        )
    }

    @Test
    fun `test that search works properly`() = runTest {
        // given
        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
        initViewModel()
        advanceUntilIdle()

        val query = "sample"

        // when
        photosSearchViewModel.search(query)

        // then
        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.query).isEqualTo(query)
        }
    }

    @Test
    fun `test that lazy dependencies are not initialized when feature flag is false`() = runTest {
        // given
        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)

        // when
        initViewModel()
        advanceUntilIdle()

        // then - PhotosCache is used instead, so lazy dependencies should not be accessed
        // The fact that we didn't set up mocks for albumsDataProvider.monitorAlbums()
        // or monitorTimelinePhotosUseCase() proves they weren't called
        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.isSingleActivityEnabled).isFalse()
        }
    }

    @Test
    fun `test that photos are fetched from PhotosCache when feature flag is disabled`() = runTest {
        val expectedPhoto = Photo.Image(
            id = 1L,
            albumPhotoId = null,
            parentId = 0L,
            name = "cached_photo.jpg",
            isFavourite = false,
            creationTime = LocalDateTime.now(),
            modificationTime = LocalDateTime.now(),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpg",
            ),
            size = 1024L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
        )
        PhotosCache.updatePhotos(listOf(expectedPhoto))

        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)

        initViewModel()
        advanceUntilIdle()

        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.isSingleActivityEnabled).isFalse()
            assertThat(state.photosSource).hasSize(1)
            assertThat(state.photosSource.first()).isEqualTo(expectedPhoto)
        }
    }

    @Test
    fun `test that search returns legacyAlbums when feature flag is disabled`() = runTest {
        val matchingAlbum = UIAlbum(
            id = Album.FavouriteAlbum,
            title = AlbumTitle.StringTitle("Favourites"),
            count = 5,
            imageCount = 3,
            videoCount = 2,
            coverPhoto = null,
            defaultCover = null,
        )
        val nonMatchingAlbum = UIAlbum(
            id = Album.GifAlbum,
            title = AlbumTitle.StringTitle("GIFs"),
            count = 10,
            imageCount = 10,
            videoCount = 0,
            coverPhoto = null,
            defaultCover = null,
        )
        PhotosCache.updateAlbums(listOf(matchingAlbum, nonMatchingAlbum))

        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
        whenever(albumTitleStringMapper(AlbumTitle.StringTitle("Favourites"))).thenReturn("Favourites")
        whenever(albumTitleStringMapper(AlbumTitle.StringTitle("GIFs"))).thenReturn("GIFs")

        initViewModel()
        advanceUntilIdle()
        photosSearchViewModel.search("Fav")
        advanceUntilIdle()

        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.isSingleActivityEnabled).isFalse()
            assertThat(state.legacyAlbums).hasSize(1)
            assertThat(state.legacyAlbums.first()).isEqualTo(matchingAlbum)
        }
    }

    @Test
    fun `test that search returns photos when feature flag is disabled`() = runTest {
        val matchingPhoto = Photo.Image(
            id = 1L,
            albumPhotoId = null,
            parentId = 0L,
            name = "vacation_beach.jpg",
            isFavourite = false,
            creationTime = LocalDateTime.now(),
            modificationTime = LocalDateTime.now(),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpg",
            ),
            size = 1024L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
        )
        val nonMatchingPhoto = Photo.Image(
            id = 2L,
            albumPhotoId = null,
            parentId = 0L,
            name = "mountain_trip.jpg",
            isFavourite = false,
            creationTime = LocalDateTime.now(),
            modificationTime = LocalDateTime.now(),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpg",
            ),
            size = 2048L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
        )
        PhotosCache.updatePhotos(listOf(matchingPhoto, nonMatchingPhoto))

        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)

        initViewModel()
        advanceUntilIdle()
        photosSearchViewModel.search("vacation")
        advanceUntilIdle()

        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.isSingleActivityEnabled).isFalse()
            assertThat(state.photos).hasSize(1)
            assertThat(state.photos.first()).isEqualTo(matchingPhoto)
        }
    }

    @Test
    fun `test that albums are fetched from albumsProvider when feature flag is enabled`() =
        runTest {
            val mediaAlbum = mock<MediaAlbum.System>()
            val expectedAlbumUiState = AlbumUiState(
                mediaAlbum = mediaAlbum,
                title = LocalizedText.Literal("Favourites"),
                isExported = false,
                cover = null,
            )

            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
            wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(true)
            whenever(albumsDataProvider.order).thenReturn(0)
            whenever(albumsDataProvider.monitorAlbums()).thenReturn(flowOf(listOf(mediaAlbum)))
            whenever(albumUiStateMapper(mediaAlbum)).thenReturn(expectedAlbumUiState)
            whenever(monitorTimelinePhotosUseCase(any())).thenReturn(flowOf())

            initViewModel()
            advanceUntilIdle()

            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.isSingleActivityEnabled).isTrue()
                assertThat(state.albumSource).hasSize(1)
                assertThat(state.albumSource.first()).isEqualTo(expectedAlbumUiState)
            }
        }

    @Test
    fun `test that photos are fetched from monitorTimelinePhotosUseCase when feature flag is enabled`() =
        runTest {
            val expectedPhoto = Photo.Image(
                id = 1L,
                albumPhotoId = null,
                parentId = 0L,
                name = "test_photo.jpg",
                isFavourite = false,
                creationTime = LocalDateTime.now(),
                modificationTime = LocalDateTime.now(),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = StaticImageFileTypeInfo(
                    mimeType = "image/jpeg",
                    extension = "jpg",
                ),
                size = 1024L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photoResult = PhotoResult(
                photo = expectedPhoto,
                isMarkedSensitive = false
            )
            val timelinePhotosResult = TimelinePhotosResult(
                allPhotos = listOf(photoResult),
                nonSensitivePhotos = listOf(photoResult),
            )

            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
            wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(true)
            whenever(albumsDataProvider.order).thenReturn(0)
            whenever(albumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
            whenever(monitorTimelinePhotosUseCase(any())).thenReturn(flowOf(timelinePhotosResult))

            initViewModel()
            advanceUntilIdle()

            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.isSingleActivityEnabled).isTrue()
                assertThat(state.photosSource).hasSize(1)
                assertThat(state.photosSource.first()).isEqualTo(expectedPhoto)
            }
        }

    @Test
    fun `test that contentState is WelcomeEmpty when query is blank and recentQueries is empty`() =
        runTest {
            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(emptyList())

            initViewModel()

            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.query).isEmpty()
                assertThat(state.recentQueries).isEmpty()
                assertThat(state.contentState).isEqualTo(MediaContentState.WelcomeEmpty)
            }
        }

    @Test
    fun `test that contentState is RecentQueries when query is blank but recentQueries has items`() =
        runTest {
            val recentQueries = listOf("query1", "query2")
            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(recentQueries)

            initViewModel()
            photosSearchViewModel.updateQuery("")

            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.query).isEmpty()
                assertThat(state.recentQueries).isEqualTo(recentQueries)
                assertThat(state.contentState).isEqualTo(MediaContentState.RecentQueries)
            }
        }

    @Test
    fun `test that contentState is NoResults when search returns no albums and no photos`() =
        runTest {
            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(emptyList())

            initViewModel()
            photosSearchViewModel.search("nonexistent")
            photosSearchViewModel.updateQuery("nonexistent")

            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.query).isEqualTo("nonexistent")
                assertThat(state.legacyAlbums).isEmpty()
                assertThat(state.photos).isEmpty()
                assertThat(state.isSearchingAlbums).isFalse()
                assertThat(state.isSearchingPhotos).isFalse()
                assertThat(state.contentState).isEqualTo(MediaContentState.NoResults)
            }
        }

    @Test
    fun `test that contentState is SearchResults when search returns albums`() = runTest {
        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(emptyList())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(true)
        whenever(albumsDataProvider.order).thenReturn(0)
        whenever(albumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        whenever(monitorTimelinePhotosUseCase(any())).thenReturn(flowOf())
        whenever(albumTitleStringMapper(any())).thenReturn("Test Album")

        initViewModel()
        photosSearchViewModel.search("Test")

        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.query).isEqualTo("Test")
        }
    }

    @Test
    fun `test that contentState is SearchResults when search returns photos`() = runTest {
        val expectedPhoto = Photo.Image(
            id = 1L,
            albumPhotoId = null,
            parentId = 0L,
            name = "vacation_photo.jpg",
            isFavourite = false,
            creationTime = LocalDateTime.now(),
            modificationTime = LocalDateTime.now(),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpg",
            ),
            size = 1024L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
        )
        val photoResult = PhotoResult(
            photo = expectedPhoto,
            isMarkedSensitive = false
        )
        val timelinePhotosResult = TimelinePhotosResult(
            allPhotos = listOf(photoResult),
            nonSensitivePhotos = listOf(photoResult),
        )

        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(emptyList())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(true)
        whenever(albumsDataProvider.order).thenReturn(0)
        whenever(albumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        whenever(monitorTimelinePhotosUseCase(any())).thenReturn(flowOf(timelinePhotosResult))

        initViewModel()
        photosSearchViewModel.search("vacation")

        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.query).isEqualTo("vacation")
            assertThat(state.photos).hasSize(1)
            assertThat(state.contentState).isEqualTo(MediaContentState.SearchResults)
        }
    }

    @Test
    fun `test that contentState updates to RecentQueries when query is cleared after having recentQueries`() =
        runTest {
            val recentQueries = listOf("previous_search")
            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(recentQueries)

            initViewModel()
            photosSearchViewModel.updateQuery("some_query")
            photosSearchViewModel.updateQuery("")
            photosSearchViewModel.updateQuery("")

            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.query).isEmpty()
                assertThat(state.recentQueries).isEqualTo(recentQueries)
                assertThat(state.contentState).isEqualTo(MediaContentState.RecentQueries)
            }
        }

    @Test
    fun `test that updateRecentQueries updates contentState correctly`() = runTest {
        whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(emptyList())

        initViewModel()

        photosSearchViewModel.state.test {
            val initialState = awaitItem()
            assertThat(initialState.contentState).isEqualTo(MediaContentState.WelcomeEmpty)
        }

        photosSearchViewModel.updateRecentQueries("new_query")
        photosSearchViewModel.updateQuery("")

        photosSearchViewModel.state.test {
            val state = awaitItem()
            assertThat(state.recentQueries).contains("new_query")
            assertThat(state.contentState).isEqualTo(MediaContentState.RecentQueries)
        }
    }
}