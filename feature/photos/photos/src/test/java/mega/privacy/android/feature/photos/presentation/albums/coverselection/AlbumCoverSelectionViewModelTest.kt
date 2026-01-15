package mega.privacy.android.feature.photos.presentation.albums.coverselection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.photos.UpdateAlbumCoverUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class AlbumCoverSelectionViewModelTest {

    private val updateAlbumCoverUseCase: UpdateAlbumCoverUseCase = mock<UpdateAlbumCoverUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val photoUiStateMapper = mock<PhotoUiStateMapper>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()

    private suspend fun ReceiveTurbine<AlbumCoverSelectionState>.awaitCondition(
        timeoutMs: Long = 1000,
        condition: (AlbumCoverSelectionState) -> Boolean,
    ): AlbumCoverSelectionState {
        var state = awaitItem()
        var attempts = 0
        val maxAttempts = 100 // Prevent infinite loops
        while (!condition(state) && attempts < maxAttempts) {
            state = awaitItem()
            attempts++
        }
        if (!condition(state)) {
            throw AssertionError("Condition not met after $attempts attempts. Last state: $state")
        }
        return state
    }

    private val accountLevelDetail = mock<AccountLevelDetail> {
        on { accountType }.thenReturn(AccountType.PRO_III)
    }
    private val accountDetail = mock<AccountDetail> {
        on { levelDetail }.thenReturn(accountLevelDetail)
    }

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

        val expectedPhotoUiStates = expectedPhotos.map { createPhotoUiState(it) }
        expectedPhotos.forEachIndexed { index, photo ->
            whenever(photoUiStateMapper(photo)).thenReturn(expectedPhotoUiStates[index])
        }
        whenever(
            fileTypeIconMapper(
                any(),
                any()
            )
        ).thenReturn(iconPackR.drawable.ic_image_medium_solid)

        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        // when
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(mapOf("album_id" to 1L)),
            getUserAlbum = { flowOf(expectedAlbum) },
            getAlbumPhotos = { flowOf(expectedPhotos) },
            downloadThumbnailUseCase = mock(),
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = mock(),
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            albumId = null,
        )

        // then
        underTest.state.test {
            val actual = awaitCondition { it.album != null && it.photos.isNotEmpty() }
            assertThat(expectedAlbum).isEqualTo(actual.album)
            val expectedSortedPhotoUiStates =
                expectedPhotoUiStates.sortedByDescending { it.modificationTime }
            assertThat(expectedSortedPhotoUiStates.map { it.id })
                .isEqualTo(actual.photos.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that select photo returns correct result`() = runTest {
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(
            fileTypeIconMapper(
                any(),
                any()
            )
        ).thenReturn(iconPackR.drawable.ic_image_medium_solid)

        val album = createUserAlbum(id = AlbumId(1L))
        val photo = createImage(id = 1L)
        val photoUiState = createPhotoUiState(photo)
        whenever(photoUiStateMapper(photo)).thenReturn(photoUiState)

        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(mapOf("album_id" to 1L)),
            getUserAlbum = { flowOf(album) },
            getAlbumPhotos = { flowOf(listOf(photo)) },
            downloadThumbnailUseCase = mock(),
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = mock(),
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            albumId = null,
        )

        // load initial state
        underTest.state.test {
            awaitCondition { it.photosNodeContentTypes.isNotEmpty() }
            cancelAndIgnoreRemainingEvents()
        }

        // when - select the photo
        underTest.selectPhoto(photoUiState)

        // then - verify the photo's isSelected flag is true in photosNodeContentTypes
        underTest.state.test {
            val stateAfterSelection =
                awaitCondition { it.hasSelectedPhoto && it.photosNodeContentTypes.isNotEmpty() }
            assertThat(stateAfterSelection.hasSelectedPhoto).isTrue()

            val selectedPhotoNode = stateAfterSelection.photosNodeContentTypes
                .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
                .firstOrNull { it.node.photo.id == photoUiState.id }

            assertThat(selectedPhotoNode).isNotNull()
            assertThat(selectedPhotoNode?.node?.isSelected).isTrue()
            assertThat(selectedPhotoNode?.node?.photo?.id).isEqualTo(photoUiState.id)

            cancelAndIgnoreRemainingEvents()
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
            getBusinessStatusUseCase = mock(),
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            albumId = null,
        )

        // then
        underTest.state.test {
            val actual = awaitCondition { it.accountType != null }
            assertThat(actual.accountType).isEqualTo(AccountType.FREE)
            assertThat(underTest.showHiddenItems).isTrue()
            cancelAndIgnoreRemainingEvents()
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

    private fun createPhotoUiState(
        photo: Photo,
    ): PhotoUiState = when (photo) {
        is Photo.Image -> PhotoUiState.Image(
            id = photo.id,
            albumPhotoId = photo.albumPhotoId,
            parentId = photo.parentId,
            name = photo.name,
            isFavourite = photo.isFavourite,
            creationTime = photo.creationTime,
            modificationTime = photo.modificationTime,
            thumbnailFilePath = photo.thumbnailFilePath,
            previewFilePath = photo.previewFilePath,
            fileTypeInfo = photo.fileTypeInfo,
            size = photo.size,
            isTakenDown = photo.isTakenDown,
            isSensitive = photo.isSensitive,
            isSensitiveInherited = photo.isSensitiveInherited,
            base64Id = photo.base64Id,
        )

        is Photo.Video -> PhotoUiState.Video(
            id = photo.id,
            albumPhotoId = photo.albumPhotoId,
            parentId = photo.parentId,
            name = photo.name,
            isFavourite = photo.isFavourite,
            creationTime = photo.creationTime,
            modificationTime = photo.modificationTime,
            thumbnailFilePath = photo.thumbnailFilePath,
            previewFilePath = photo.previewFilePath,
            fileTypeInfo = photo.fileTypeInfo,
            size = photo.size,
            isTakenDown = photo.isTakenDown,
            isSensitive = photo.isSensitive,
            isSensitiveInherited = photo.isSensitiveInherited,
            base64Id = photo.base64Id,
            duration = "",
        )
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
