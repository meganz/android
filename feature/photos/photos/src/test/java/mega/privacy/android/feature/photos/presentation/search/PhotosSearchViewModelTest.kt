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
import mega.privacy.android.feature.photos.mapper.UIAlbumMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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

    private val uiAlbumMapper: UIAlbumMapper = mock()

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

        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(flowOf(false))
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(flowOf(accountDetail))
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
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
            uiAlbumMapper = uiAlbumMapper,
            monitorTimelinePhotosUseCase = lazyMonitorTimelinePhotosUseCase,
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
    fun `test that albums are fetched from albumsProvider when feature flag is enabled`() =
        runTest {
            // given
            val expectedUIAlbum = UIAlbum(
                id = Album.FavouriteAlbum,
                title = AlbumTitle.StringTitle("Favourites"),
                count = 0,
                imageCount = 0,
                videoCount = 0,
                coverPhoto = null,
                defaultCover = null,
            )
            val mediaAlbum = mock<MediaAlbum.System>()

            whenever(retrievePhotosRecentQueriesUseCase()).thenReturn(listOf())
            wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(true)
            whenever(albumsDataProvider.order).thenReturn(0)
            whenever(albumsDataProvider.monitorAlbums()).thenReturn(flowOf(listOf(mediaAlbum)))
            whenever(uiAlbumMapper(mediaAlbum)).thenReturn(expectedUIAlbum)
            whenever(monitorTimelinePhotosUseCase(any())).thenReturn(flowOf())
            // when
            initViewModel()
            advanceUntilIdle()

            // then
            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.isSingleActivityEnabled).isTrue()
                assertThat(state.albumsSource).hasSize(1)
                assertThat(state.albumsSource.first()).isEqualTo(expectedUIAlbum)
            }
        }

    @Test
    fun `test that photos are fetched from monitorTimelinePhotosUseCase when feature flag is enabled`() =
        runTest {
            // given
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
                isMarkedSensitive = false,
                inTypedNode = null,
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

            // when
            initViewModel()
            advanceUntilIdle()

            // then
            photosSearchViewModel.state.test {
                val state = awaitItem()
                assertThat(state.isSingleActivityEnabled).isTrue()
                assertThat(state.photosSource).hasSize(1)
                assertThat(state.photosSource.first()).isEqualTo(expectedPhoto)
            }
        }
}