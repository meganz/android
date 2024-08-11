package test.mega.privacy.android.app.presentation.photos.albums.coverselection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.albums.coverselection.AlbumCoverSelectionViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.UpdateAlbumCoverUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class AlbumCoverSelectionViewModelTest {

    private val updateAlbumCoverUseCase: UpdateAlbumCoverUseCase = mock<UpdateAlbumCoverUseCase>()

    @Test
    fun `test that fetch album and photos returns correct result`() = runTest {
        // given
        val expectedAlbum = createUserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
        )

        val expectedPhotos = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )

        // when
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_ID to 1L)),
            getUserAlbum = { flowOf(expectedAlbum) },
            getAlbumPhotos = { flowOf(expectedPhotos) },
            downloadThumbnailUseCase = mock(),
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = mock {
                onBlocking { invoke(any()) }.thenReturn(false)
            },
            monitorShowHiddenItemsUseCase = mock(),
            monitorAccountDetailUseCase = mock(),
        )

        // then
        underTest.state.drop(1).test {
            val actualAlbum = awaitItem().album
            assertThat(expectedAlbum).isEqualTo(actualAlbum)

            val actualPhotos = awaitItem().photos
            assertThat(expectedPhotos.sortedByDescending { it.modificationTime })
                .isEqualTo(actualPhotos)
        }
    }

    @Test
    fun `test that select photo returns correct result`() = runTest {
        // given
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            getUserAlbum = { flowOf() },
            getAlbumPhotos = { flowOf() },
            downloadThumbnailUseCase = mock(),
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = mock {
                onBlocking { invoke(any()) }.thenReturn(false)
            },
            monitorShowHiddenItemsUseCase = mock(),
            monitorAccountDetailUseCase = mock(),
        )

        val expectedPhoto = createImage(id = 1L)

        // when
        underTest.selectPhoto(expectedPhoto)

        // then
        underTest.state.test {
            val actualPhoto = awaitItem().selectedPhoto
            assertThat(expectedPhoto).isEqualTo(actualPhoto)
        }
    }

    @Test
    fun `test that update cover returns correct result`() = runTest {
        // given
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            getUserAlbum = { flowOf() },
            getAlbumPhotos = { flowOf() },
            downloadThumbnailUseCase = mock(),
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = mock {
                onBlocking { invoke(any()) }.thenReturn(false)
            },
            monitorShowHiddenItemsUseCase = mock(),
            monitorAccountDetailUseCase = mock(),
        )

        val album = createUserAlbum(id = AlbumId(1L))
        val photo = createImage(id = 2L, albumPhotoId = 2L)

        // when
        underTest.updateCover(album, photo)

        // then
        underTest.state.drop(1).test {
            val isCompleted = awaitItem().isSelectionCompleted
            assertThat(isCompleted).isTrue()
        }
    }

    @Test
    fun `test that showHiddenItems and accountDetail are fetched properly`() = runTest {
        // given
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            getUserAlbum = { flowOf() },
            getAlbumPhotos = { flowOf() },
            downloadThumbnailUseCase = mock(),
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = mock {
                onBlocking { invoke(AppFeatures.HiddenNodes) }.thenReturn(true)
            },
            monitorShowHiddenItemsUseCase = mock {
                on { invoke() }.thenReturn(flowOf(true))
            },
            monitorAccountDetailUseCase = mock {
                on { invoke() }.thenReturn(
                    flowOf(
                        AccountDetail(
                            levelDetail = AccountLevelDetail(
                                accountType = AccountType.FREE,
                                subscriptionStatus = null,
                                subscriptionRenewTime = 0L,
                                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                                proExpirationTime = 0L,
                                accountPlanDetail = null,
                                accountSubscriptionDetailList = listOf(),
                            )
                        )
                    )
                )
            },
        )
        advanceUntilIdle()

        // then
        underTest.state.test {
            val accountType = awaitItem().accountType
            assertThat(accountType).isEqualTo(AccountType.FREE)
            assertThat(underTest.showHiddenItems).isTrue()
        }
    }

    private fun createUserAlbum(
        id: AlbumId,
        title: String = "",
        cover: Photo? = null,
        creationTime: Long = 0L,
        modificationTime: Long = 0L,
        isExported: Boolean = false,
    ) = Album.UserAlbum(id, title, cover, creationTime, modificationTime, isExported)

    private fun createImage(
        id: Long,
        albumPhotoId: Long? = null,
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        fileTypeInfo: FileTypeInfo = UnknownFileTypeInfo(mimeType = "", extension = ""),
    ): Photo = Photo.Image(
        id,
        albumPhotoId,
        parentId,
        name,
        isFavourite,
        creationTime,
        modificationTime,
        thumbnailFilePath,
        previewFilePath,
        fileTypeInfo,
    )

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
