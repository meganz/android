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
import mega.privacy.android.app.presentation.photos.search.PhotosSearchViewModel
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.RetrievePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.SavePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import org.junit.Before
import org.junit.Test
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

        photosSearchViewModel = PhotosSearchViewModel(
            defaultDispatcher = defaultDispatcher,
            albumTitleStringMapper = albumTitleStringMapper,
            retrievePhotosRecentQueriesUseCase = retrievePhotosRecentQueriesUseCase,
            savePhotosRecentQueriesUseCase = savePhotosRecentQueriesUseCase,
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
