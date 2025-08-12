package mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.CreateAlbumUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetDefaultAlbumsMapUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumsViewModelTest {
    private lateinit var underTest: AlbumsViewModel

    private val getDefaultAlbumPhotos = mock<GetDefaultAlbumPhotos>()
    private val uiAlbumMapper = UIAlbumMapper()
    private val getUserAlbums = mock<GetUserAlbums>()
    private val getAlbumPhotos = mock<GetAlbumPhotos>()
    private val getDefaultAlbumsMapUseCase = mock<GetDefaultAlbumsMapUseCase>()
    private val getProscribedAlbumNamesUseCase = mock<GetProscribedAlbumNamesUseCase>()
    private val createAlbumUseCase = mock<CreateAlbumUseCase>()
    private val removeAlbumsUseCase = mock<RemoveAlbumsUseCase>()
    private val disableExportAlbumsUseCase: DisableExportAlbumsUseCase = mock()
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase = mock()
    private val proscribedStrings =
        listOf("My albums", "Shared albums", "Favourites", "RAW", "GIFs")
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock {
        on { invoke() }.thenReturn(flowOf(true))
    }
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock {
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
    }
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    @BeforeEach
    fun setUp() {
        whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(listOf()))
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = AlbumsViewModel(
            getDefaultAlbumPhotos = getDefaultAlbumPhotos,
            getDefaultAlbumsMapUseCase = getDefaultAlbumsMapUseCase,
            getUserAlbums = getUserAlbums,
            getAlbumPhotos = getAlbumPhotos,
            getProscribedAlbumNamesUseCase = getProscribedAlbumNamesUseCase,
            uiAlbumMapper = uiAlbumMapper,
            createAlbumUseCase = createAlbumUseCase,
            removeAlbumsUseCase = removeAlbumsUseCase,
            disableExportAlbumsUseCase = disableExportAlbumsUseCase,
            defaultDispatcher = dispatcher,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getDefaultAlbumPhotos,
            getDefaultAlbumsMapUseCase,
            getUserAlbums,
            getAlbumPhotos,
            getProscribedAlbumNamesUseCase,
            createAlbumUseCase,
            removeAlbumsUseCase,
            disableExportAlbumsUseCase,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.albums).isEmpty()
        }
    }

    @Test
    fun `test that an error would return an empty list`() = runTest {
        whenever(
            getDefaultAlbumPhotos(
                false,
                listOf()
            )
        ).thenReturn(flow { throw Exception("Error") })

        underTest.state.test {
            assertEquals(emptyList(), awaitItem().albums)
        }
    }

    @Test
    fun `test that returned albums are added to the state if there are photos for them`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { true },
                Album.RawAlbum to { true },
            )

            whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
            whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(listOf(createImage())))
            initUnderTest()

            underTest.state.drop(1).test {
                assertThat(awaitItem().albums.map { it.id }).containsExactlyElementsIn(defaultAlbums.keys)
            }
        }

    @Test
    fun `test that albums are not added, if there are no photos in them`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { true },
            Album.RawAlbum to { false },
        )

        whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
        whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(listOf(createImage())))

        initUnderTest()

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.id })
                .containsExactlyElementsIn(defaultAlbums.keys.filterNot { it == Album.RawAlbum })
        }
    }

    @Test
    fun `test that favourite album is displayed even if it contains no photos`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { false },
            Album.GifAlbum to { false },
            Album.RawAlbum to { false },
        )

        whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
        whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(emptyList()))

        initUnderTest()

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.id })
                .containsExactlyElementsIn(defaultAlbums.keys.filter { it == Album.FavouriteAlbum })
        }
    }

    @Test
    fun `test that feature flag filters out the correct albums`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { true },
            Album.RawAlbum to { true },
        )

        whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
        whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(emptyList()))
        initUnderTest()
        underTest.state.drop(1).test {
            val albums = awaitItem().albums
            assertEquals(albums.size, 1)
            assertThat(albums.first().id).isEqualTo(Album.FavouriteAlbum)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that album is using latest modification time photo`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { false },
            Album.RawAlbum to { false },
        )

        val testPhotosList = listOf(
            createImage(id = 1L, modificationTime = LocalDateTime.MAX),
            createImage(id = 2L, modificationTime = LocalDateTime.MIN)
        )

        whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
        whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(testPhotosList))
        initUnderTest()
        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.coverPhoto?.id }.firstOrNull()).isEqualTo(1L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that user albums collect is working and the sorting order is correct`() = runTest {
        val newAlbum1 =
            createUserAlbum(id = AlbumId(1L), title = "Album 1", creationTime = 100L)
        val newAlbum2 =
            createUserAlbum(id = AlbumId(2L), title = "Album 2", creationTime = 200L)
        val newAlbum3 =
            createUserAlbum(id = AlbumId(3L), title = "Album 3", creationTime = 300L)

        whenever(getUserAlbums()).thenReturn(
            flowOf(
                listOf(
                    newAlbum1, newAlbum2, newAlbum3
                )
            )
        )
        whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))

        val expectedUserAlbums = listOf(
            createUserAlbum(id = AlbumId(3L), title = "Album 3", creationTime = 300L),
            createUserAlbum(id = AlbumId(2L), title = "Album 2", creationTime = 200L),
            createUserAlbum(id = AlbumId(1L), title = "Album 1", creationTime = 100L),
        )

        underTest.state.drop(1).test {
            val actualUserAlbums = awaitItem().albums.map { it.id }
            assertThat(expectedUserAlbums).isEqualTo(actualUserAlbums)
        }
    }

    @Test
    fun `test that create album returns an album with the right name`() = runTest {
        val expectedAlbumName = "Album 1"

        whenever(createAlbumUseCase(expectedAlbumName)).thenReturn(
            createUserAlbum(title = expectedAlbumName)
        )
        whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedStrings)

        underTest.createNewAlbum(expectedAlbumName)

        underTest.state.drop(1).test {
            awaitItem()
            val actualAlbum = awaitItem().currentAlbum as Album.UserAlbum
            assertEquals(expectedAlbumName, actualAlbum.title)
        }
    }

    @Test
    fun `test that an error in creating an album would keep current album as null`() = runTest {
        whenever(createAlbumUseCase(any())).thenAnswer { throw Exception() }

        underTest.createNewAlbum("ABD")

        underTest.state.test {
            assertNull(awaitItem().currentAlbum)
        }
    }

    @Test
    fun `test that setPlaceholderAlbumTitle set the right text if no album with default name exists`() =
        runTest {
            val expectedName = "New album"
            whenever(getUserAlbums()).thenReturn(flowOf(listOf()))
            whenever(
                getNextDefaultAlbumNameUseCase(
                    any(),
                    any()
                )
            ).thenReturn(expectedName)
            underTest.setPlaceholderAlbumTitle(expectedName)

            underTest.state.test {
                val actualName = awaitItem().createAlbumPlaceholderTitle
                assertThat(actualName).isEqualTo(expectedName)
            }
        }

    @Test
    fun `test that setPlaceholderAlbumTitle set the right text if an album with default name already exists`() =
        runTest {
            val expectedName = "New album (1)"
            val albumName = "New album"
            val newUserAlbum = createUserAlbum(title = albumName)
            whenever(getUserAlbums()).thenReturn(
                flowOf(
                    listOf(
                        newUserAlbum
                    )
                )
            )
            whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))
            whenever(
                getNextDefaultAlbumNameUseCase(
                    any(),
                    any()
                )
            ).thenReturn(expectedName)
            underTest.state.drop(1).test {
                underTest.setPlaceholderAlbumTitle(albumName)
                val actualName = awaitItem().createAlbumPlaceholderTitle
                assertThat(actualName).isEqualTo(expectedName)
            }
        }

    @Test
    fun `test that setPlaceholderAlbumTitle set the right text if two albums with default name already exist`() =
        runTest {
            val expectedName = "New album (2)"
            val newAlbum1 =
                createUserAlbum(id = AlbumId(1L), title = "New album", modificationTime = 1L)
            val newAlbum2 =
                createUserAlbum(id = AlbumId(2L), title = "New album (1)", modificationTime = 2L)

            whenever(getUserAlbums()).thenReturn(
                flowOf(
                    listOf(
                        newAlbum1,
                        newAlbum2
                    )
                )
            )
            whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))
            whenever(
                getNextDefaultAlbumNameUseCase(
                    any(),
                    any()
                )
            ).thenReturn(expectedName)
            underTest.setPlaceholderAlbumTitle("New album")

            underTest.state.drop(1).test {
                val actualName = awaitItem().createAlbumPlaceholderTitle
                assertEquals(expectedName, actualName)
            }
        }

    @Test
    fun `test that creating an album with a system album title will not create the album`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { false },
                Album.RawAlbum to { false },
            )

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedStrings)
            whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
            whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(emptyList()))

            underTest.state.test {
                awaitItem()
                underTest.createNewAlbum("favourites")
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    R.string.photos_create_album_error_message_systems_album,
                    item.createDialogErrorMessage
                )
            }
            verifyNoInteractions(createAlbumUseCase)

        }

    @Test
    fun `test that creating an album with an empty system album title will not create the album`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { false },
                Album.RawAlbum to { false },
            )

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedStrings)
            whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
            whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(emptyList()))

            underTest.state.test {
                awaitItem()
                underTest.createNewAlbum("raw")
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    R.string.photos_create_album_error_message_systems_album,
                    item.createDialogErrorMessage
                )
            }
            verifyNoInteractions(createAlbumUseCase)

        }

    @Test
    fun `test that creating an album with all spaces will not create the album`() =
        runTest {
            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedStrings)

            underTest.state.test {
                awaitItem()
                underTest.createNewAlbum("      ")
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    R.string.invalid_string,
                    item.createDialogErrorMessage
                )
            }
            verifyNoInteractions(createAlbumUseCase)
        }

    @Test
    fun `test that creating an album with an existing title will not create the album`() =
        runTest {
            val testAlbumName = "Album 1"
            val newAlbum1 = createUserAlbum(title = testAlbumName)

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedStrings)
            whenever(getUserAlbums()).thenReturn(flowOf(listOf(newAlbum1)))
            whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))

            underTest.state.drop(1).test {
                awaitItem()
                underTest.createNewAlbum(testAlbumName)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    item.createDialogErrorMessage,
                    R.string.photos_create_album_error_message_duplicate
                )
            }
            verifyNoInteractions(createAlbumUseCase)
        }

    @Test
    fun `test that creating an album with an invalid character will not create the album`() =
        runTest {
            val testAlbumName = "*"

            whenever(getProscribedAlbumNamesUseCase()).thenReturn(proscribedStrings)

            underTest.state.test {
                awaitItem()
                underTest.createNewAlbum(testAlbumName)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    item.createDialogErrorMessage,
                    R.string.invalid_characters_defined
                )
            }

            verifyNoInteractions(createAlbumUseCase)
        }

    @Test
    fun `test that albums are deleted properly`() = runTest {
        whenever(removeAlbumsUseCase(any())).thenReturn(Unit)

        val expectedDeletedAlbumIds = setOf(
            AlbumId(1L),
            AlbumId(2L),
            AlbumId(3L),
        )
        underTest.deleteAlbums(expectedDeletedAlbumIds.toList())

        underTest.state.test {
            val actualDeletedAlbumIds = awaitItem().deletedAlbumIds
            assertEquals(expectedDeletedAlbumIds, actualDeletedAlbumIds)
        }
    }

    @Test
    fun `test that album deleted message is updated properly`() = runTest {
        val expectedMessage = "Album deleted"
        underTest.updateAlbumDeletedMessage(expectedMessage)

        underTest.state.test {
            val actualMessage = awaitItem().albumDeletedMessage
            assertEquals(expectedMessage, actualMessage)
        }
    }

    @Test
    fun `test that delete album confirmation is shown properly`() = runTest {
        underTest.showDeleteAlbumsConfirmation()
        underTest.state.test {
            val showDeleteAlbumsConfirmation = awaitItem().showDeleteAlbumsConfirmation
            assertThat(showDeleteAlbumsConfirmation).isTrue()
        }
    }

    @Test
    fun `test that delete album confirmation is closed properly`() = runTest {
        underTest.closeDeleteAlbumsConfirmation()
        underTest.state.test {
            val showDeleteAlbumsConfirmation = awaitItem().showDeleteAlbumsConfirmation
            assertThat(showDeleteAlbumsConfirmation).isFalse()
        }
    }

    @Test
    fun `test that album is selected properly`() = runTest {
        // given
        val expectedAlbum = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        // when
        underTest.selectAlbum(expectedAlbum)

        // then
        underTest.state.drop(1).test {
            val selectedAlbumIds = awaitItem().selectedAlbumIds
            assertThat(expectedAlbum.id in selectedAlbumIds).isTrue()
        }
    }

    @Test
    fun `test that album is unselected properly`() = runTest {
        // given
        val expectedAlbum = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        // when
        underTest.selectAlbum(expectedAlbum)
        underTest.unselectAlbum(expectedAlbum)

        // then
        underTest.state.test {
            val selectedAlbumIds = awaitItem().selectedAlbumIds
            assertThat(expectedAlbum.id !in selectedAlbumIds).isTrue()
        }
    }

    @Test
    fun `test that album is cleared properly`() = runTest {
        // given
        val expectedAlbum1 = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        val expectedAlbum2 = Album.UserAlbum(
            id = AlbumId(2L),
            title = "Album 2",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        // when
        underTest.selectAlbum(expectedAlbum1)
        underTest.selectAlbum(expectedAlbum2)
        underTest.clearAlbumSelection()

        // then
        underTest.state.test {
            val selectedAlbumIds = awaitItem().selectedAlbumIds
            assertThat(selectedAlbumIds.isEmpty()).isTrue()
        }
    }

    @Test
    fun `test that refresh album works correctly`() = runTest {
        // given
        underTest.systemAlbumPhotos[Album.FavouriteAlbum] = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )

        underTest.userAlbums[AlbumId(1L)] = createUserAlbum()
        underTest.userAlbumPhotos[AlbumId(1L)] = listOf(
            createImage(id = 4L),
            createImage(id = 5L),
            createImage(id = 6L),
        )

        underTest.userAlbums[AlbumId(2L)] = createUserAlbum()
        underTest.userAlbumPhotos[AlbumId(2L)] = listOf(
            createImage(id = 7L),
            createImage(id = 8L),
            createImage(id = 9L),
        )

        // when
        underTest.refreshAlbums()

        // then
        underTest.state.test {
            val albums = awaitItem().albums
            assertThat(albums.size).isEqualTo(3)
        }
    }

    @Test
    fun `test that showHiddenItems and accountDetail are fetched properly`() = runTest {
        // given
        wheneverBlocking { getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease) }.thenReturn(
            true
        )
        initUnderTest()
        advanceUntilIdle()

        // then
        underTest.state.test {
            val accountType = awaitItem().accountType
            assertThat(accountType).isEqualTo(AccountType.FREE)
            assertThat(underTest.showHiddenItems).isTrue()
        }
    }

    @Test
    fun `test that on pagination enabled should invoke use case with correct parameters`() =
        runTest {
            val isPaginationEnabled = true
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { false },
                Album.GifAlbum to { false },
                Album.RawAlbum to { false },
            )

            whenever(getDefaultAlbumsMapUseCase()).thenReturn(defaultAlbums)
            whenever(getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)).thenReturn(
                isPaginationEnabled
            )

            initUnderTest()
            advanceUntilIdle()

            verify(getDefaultAlbumPhotos).invoke(
                isPaginationEnabled,
                defaultAlbums.values.toList()
            )
        }

    private fun createUserAlbum(
        id: AlbumId = AlbumId(0L),
        title: String = "",
        cover: Photo? = null,
        creationTime: Long = 0L,
        modificationTime: Long = 0L,
        isExported: Boolean = false,
    ): Album.UserAlbum =
        Album.UserAlbum(id, title, cover, creationTime, modificationTime, isExported)

    private fun createImage(
        id: Long = 2L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
        fileTypeInfo: FileTypeInfo = StaticImageFileTypeInfo("", ""),
    ): Photo = Photo.Image(
        id = id,
        parentId = parentId,
        name = "",
        isFavourite = isFavourite,
        creationTime = LocalDateTime.now(),
        modificationTime = modificationTime,
        thumbnailFilePath = "thumbnailFilePath",
        previewFilePath = "previewFilePath",
        fileTypeInfo = fileTypeInfo
    )

    companion object {
        private val dispatcher = StandardTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(dispatcher)
    }
}
