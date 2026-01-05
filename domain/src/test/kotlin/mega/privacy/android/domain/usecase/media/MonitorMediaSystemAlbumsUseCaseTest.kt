package mega.privacy.android.domain.usecase.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * Test class for [MonitorMediaSystemAlbumsUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorMediaSystemAlbumsUseCaseTest {
    private lateinit var underTest: MonitorMediaSystemAlbumsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    private val photosRepository: PhotosRepository = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val mockSystemAlbums = setOf(
        createMockSystemAlbum(1) { photo -> photo.name.endsWith(".gif") },
        createMockSystemAlbum(2) { photo -> photo.name.endsWith(".raw") },
        createMockSystemAlbum(3) { photo -> photo.isFavourite }
    )

    @BeforeAll
    fun init() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        reset(photosRepository, monitorShowHiddenItemsUseCase, monitorAccountDetailUseCase)
    }

    private fun initUseCase() {
        underTest = MonitorMediaSystemAlbumsUseCase(
            photosRepository = photosRepository,
            systemAlbums = mockSystemAlbums,
            defaultDispatcher = testDispatcher,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase
        )
    }

    @Test
    fun `test that system albums are returned with cover photos`() = runTest {
        val mockPhotos = createMockPhotos()
        val gifPhoto = mockPhotos[0]
        val rawPhoto = mockPhotos[1]
        val favouritePhoto = mockPhotos[2]

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

        initUseCase()

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[0].cover).isEqualTo(gifPhoto)
            assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[1].cover).isEqualTo(rawPhoto)
            assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[2].cover).isEqualTo(favouritePhoto)

            awaitComplete()
        }
    }

    @Test
    fun `test that system albums are returned with null covers when no matching photos found`() =
        runTest {
            val mockPhotos = createMockPhotos()

            // Create system albums that don't match any photos
            val nonMatchingSystemAlbums = setOf(
                createMockSystemAlbum(1) { photo -> false },
                createMockSystemAlbum(2) { photo -> false },
                createMockSystemAlbum(3) { photo -> false }
            )

            whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

            underTest = MonitorMediaSystemAlbumsUseCase(
                photosRepository = photosRepository,
                systemAlbums = nonMatchingSystemAlbums,
                defaultDispatcher = testDispatcher,
                monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
                monitorAccountDetailUseCase = monitorAccountDetailUseCase
            )

            underTest().test {
                val result = awaitItem()

                assertThat(result).hasSize(3)
                assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[0].cover).isNull()
                assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[1].cover).isNull()
                assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[2].cover).isNull()

                awaitComplete()
            }
        }

    @Test
    fun `test that system albums are returned with null covers when no photos available`() =
        runTest {
            whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(emptyList()))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

            initUseCase()

            underTest().test {
                val result = awaitItem()

                assertThat(result).hasSize(3)
                assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[0].cover).isNull()
                assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[1].cover).isNull()
                assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
                assertThat(result[2].cover).isNull()

                awaitComplete()
            }
        }

    @Test
    fun `test that system albums are returned with mixed cover photos`() = runTest {
        val mockPhotos = createMockPhotos()
        val gifPhoto = mockPhotos[0]
        val favouritePhoto = mockPhotos[2]

        // Create system albums where only GIF and Favourite match
        val mixedSystemAlbums = setOf(
            createMockSystemAlbum(1) { photo -> photo.name.endsWith(".gif") },
            createMockSystemAlbum(2) { photo -> false }, // No RAW matches
            createMockSystemAlbum(3) { photo -> photo.isFavourite }
        )

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

        underTest = MonitorMediaSystemAlbumsUseCase(
            photosRepository = photosRepository,
            systemAlbums = mixedSystemAlbums,
            defaultDispatcher = testDispatcher,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase
        )

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[0].cover).isEqualTo(gifPhoto)
            assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[1].cover).isNull()
            assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[2].cover).isEqualTo(favouritePhoto)

            awaitComplete()
        }
    }

    @Test
    fun `test that system albums are returned in correct order`() = runTest {
        val mockPhotos = createMockPhotos()
        val gifPhoto = mockPhotos[0]
        val rawPhoto = mockPhotos[1]
        val favouritePhoto = mockPhotos[2]

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

        initUseCase()

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)

            awaitComplete()
        }
    }

    @Test
    fun `test that use case handles multiple matching photos by returning first match`() = runTest {
        val mockPhotos = createMockPhotos() + createMockPhotos() // Duplicate photos
        val firstGifPhoto = mockPhotos[0]

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

        initUseCase()

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat(result[0].cover).isEqualTo(firstGifPhoto)

            awaitComplete()
        }
    }

    @Test
    fun `test that free account filters out sensitive photos`() = runTest {
        val mockPhotos = createMockPhotosWithSensitive()
        val nonSensitiveGifPhoto = mockPhotos[0] // Non-sensitive GIF photo

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = false)))

        initUseCase()

        underTest().test {
            val result = awaitItem()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            // Should only find non-sensitive photos
            assertThat(result[0].cover).isEqualTo(nonSensitiveGifPhoto)
            // Sensitive photos should be filtered out
            assertThat(result[1].cover).isNull() // RAW photo is sensitive
            assertThat(result[2].cover).isNull() // Favourite photo is sensitive

            awaitComplete()
        }
    }

    @Test
    fun `test that paid account with showHiddenItems false filters out sensitive photos`() =
        runTest {
            val mockPhotos = createMockPhotosWithSensitive()
            val nonSensitiveGifPhoto = mockPhotos[0] // Non-sensitive GIF photo

            whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

            initUseCase()

            underTest().test {
                val result = awaitItem()

                assertThat(result).hasSize(3)
                assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
                // Should filter out sensitive photos when showHiddenItems = false (even for paid accounts)
                assertThat(result[0].cover).isEqualTo(nonSensitiveGifPhoto)
                assertThat(result[1].cover).isNull() // RAW photo is sensitive
                assertThat(result[2].cover).isNull() // Favourite photo is sensitive

                awaitComplete()
            }
        }

    @Test
    fun `test that paid account with showHiddenItems true includes all photos`() =
        runTest {
            val mockPhotos = createMockPhotosWithSensitive()
            val gifPhoto = mockPhotos[0]
            val rawPhoto = mockPhotos[1]
            val favouritePhoto = mockPhotos[2]

            whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid = true)))

            initUseCase()

            underTest().test {
                val result = awaitItem()

                assertThat(result).hasSize(3)
                assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
                // Should include all photos (including sensitive ones) when paid account and showHiddenItems = true
                assertThat(result[0].cover).isEqualTo(gifPhoto)
                assertThat(result[1].cover).isEqualTo(rawPhoto)
                assertThat(result[2].cover).isEqualTo(favouritePhoto)

                awaitComplete()
            }
        }

    private fun createMockSystemAlbum(resId: Int, filter: (Photo) -> Boolean): SystemAlbum {
        return object : SystemAlbum {
            override val albumNameResId = resId
            override suspend fun filter(photo: Photo): Boolean = filter(photo)
        }
    }

    private fun createMockPhotos(): List<Photo.Image> {
        return listOf(
            mock<Photo.Image> {
                on { id }.thenReturn(1L)
                on { name }.thenReturn("gif_photo.gif")
                on { isFavourite }.thenReturn(false)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            },
            mock<Photo.Image> {
                on { id }.thenReturn(2L)
                on { name }.thenReturn("raw_photo.raw")
                on { isFavourite }.thenReturn(false)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            },
            mock<Photo.Image> {
                on { id }.thenReturn(3L)
                on { name }.thenReturn("favourite_photo.jpg")
                on { isFavourite }.thenReturn(true)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            }
        )
    }

    private fun createMockPhotosWithSensitive(): List<Photo.Image> {
        return listOf(
            mock<Photo.Image> {
                on { id }.thenReturn(1L)
                on { name }.thenReturn("gif_photo.gif")
                on { isFavourite }.thenReturn(false)
                on { isSensitive }.thenReturn(false)
                on { isSensitiveInherited }.thenReturn(false)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            },
            mock<Photo.Image> {
                on { id }.thenReturn(2L)
                on { name }.thenReturn("raw_photo.raw")
                on { isFavourite }.thenReturn(false)
                on { isSensitive }.thenReturn(true)
                on { isSensitiveInherited }.thenReturn(false)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            },
            mock<Photo.Image> {
                on { id }.thenReturn(3L)
                on { name }.thenReturn("favourite_photo.jpg")
                on { isFavourite }.thenReturn(true)
                on { isSensitive }.thenReturn(false)
                on { isSensitiveInherited }.thenReturn(true)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            }
        )
    }

    private fun createMockAccountDetail(isPaid: Boolean): AccountDetail {
        val accountType = if (isPaid) AccountType.PRO_I else AccountType.FREE
        val accountLevelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = null,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = 0L,
            accountPlanDetail = null,
            accountSubscriptionDetailList = emptyList()
        )

        return AccountDetail(
            levelDetail = accountLevelDetail
        )
    }
}