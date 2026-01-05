package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetUserAlbumCoverPhotoUseCaseTest {

    private lateinit var underTest: GetUserAlbumCoverPhotoUseCase

    private val albumRepository: AlbumRepository = mock()
    private val photosRepository: PhotosRepository = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            albumRepository,
            photosRepository,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase
        )
    }

    private fun initUseCase() {
        underTest = GetUserAlbumCoverPhotoUseCase(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }

    private fun setupDefaultHiddenItemsConfig(
        showHiddenItems: Boolean = true,
        isPaid: Boolean = true,
    ) {
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(showHiddenItems))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createMockAccountDetail(isPaid)))
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
        return AccountDetail(levelDetail = accountLevelDetail)
    }

    @Test
    fun `test that null is returned when album has no elements`() = runTest {
        val albumId = AlbumId(1L)
        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(emptyList())

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isNull()
        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = false)
    }

    @Test
    fun `test that selected cover element is used when present`() = runTest {
        val albumId = AlbumId(1L)
        val selectedCoverId = 100L
        val first = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val selected = albumPhotoId(id = 100L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(first, selected)
        val expectedPhoto = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = selected.nodeId,
                albumPhotoId = selected,
                refresh = false
            )
        ).thenReturn(expectedPhoto)

        initUseCase()
        val result = underTest(albumId, selectedCoverId = selectedCoverId, refresh = false)

        assertThat(result).isEqualTo(expectedPhoto)
        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(selected.nodeId, selected, false)
    }

    @Test
    fun `test that last element is used when no cover is set`() = runTest {
        val albumId = AlbumId(2L)
        val first = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val last = albumPhotoId(id = 20L, nodeId = 3000L, albumId = albumId.id)
        val elements = listOf(first, last)
        val expectedPhoto = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = last.nodeId,
                albumPhotoId = last,
                refresh = false
            )
        ).thenReturn(expectedPhoto)

        initUseCase()
        val result = underTest(albumId, selectedCoverId = null, refresh = false)

        assertThat(result).isEqualTo(expectedPhoto)
        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(last.nodeId, last, false)
    }

    @Test
    fun `test that refresh flag is forwarded to both repositories`() = runTest {
        val albumId = AlbumId(3L)
        val last = albumPhotoId(id = 30L, nodeId = 4000L, albumId = albumId.id)
        val expectedPhoto = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = true))
            .thenReturn(listOf(last))
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = last.nodeId,
                albumPhotoId = last,
                refresh = true
            )
        ).thenReturn(expectedPhoto)

        initUseCase()
        underTest(albumId, selectedCoverId = null, refresh = true)

        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = true)
        verify(photosRepository).getPhotoFromNodeID(last.nodeId, last, true)
    }

    @Test
    fun `test that sensitive cover is filtered out for free account`() = runTest {
        val albumId = AlbumId(1L)
        val sensitivePhoto = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val nonSensitivePhoto = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(nonSensitivePhoto, sensitivePhoto)

        val sensitivePhotoMock = createMockPhoto(isSensitive = true, isSensitiveInherited = false)
        val nonSensitivePhotoMock =
            createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = false)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(photosRepository.getPhotoFromNodeID(sensitivePhoto.nodeId, sensitivePhoto, false))
            .thenReturn(sensitivePhotoMock)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nonSensitivePhoto.nodeId,
                nonSensitivePhoto,
                false
            )
        )
            .thenReturn(nonSensitivePhotoMock)

        initUseCase()
        val result = underTest(albumId, selectedCoverId = sensitivePhoto.id, refresh = false)

        // Should skip the sensitive photo and return the non-sensitive one
        assertThat(result).isEqualTo(nonSensitivePhotoMock)
    }

    @Test
    fun `test that sensitive inherited cover is filtered out for free account`() = runTest {
        val albumId = AlbumId(1L)
        val sensitiveInheritedPhoto = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val nonSensitivePhoto = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(nonSensitivePhoto, sensitiveInheritedPhoto)

        val sensitiveInheritedPhotoMock =
            createMockPhoto(isSensitive = false, isSensitiveInherited = true)
        val nonSensitivePhotoMock =
            createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = false)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                sensitiveInheritedPhoto.nodeId,
                sensitiveInheritedPhoto,
                false
            )
        )
            .thenReturn(sensitiveInheritedPhotoMock)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nonSensitivePhoto.nodeId,
                nonSensitivePhoto,
                false
            )
        )
            .thenReturn(nonSensitivePhotoMock)

        initUseCase()
        val result =
            underTest(albumId, selectedCoverId = sensitiveInheritedPhoto.id, refresh = false)

        // Should skip the sensitive inherited photo and return the non-sensitive one
        assertThat(result).isEqualTo(nonSensitivePhotoMock)
    }

    @Test
    fun `test that sensitive cover is filtered out for paid account with showHiddenItems false`() =
        runTest {
            val albumId = AlbumId(1L)
            val sensitivePhoto = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
            val nonSensitivePhoto = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
            val elements = listOf(nonSensitivePhoto, sensitivePhoto)

            val sensitivePhotoMock =
                createMockPhoto(isSensitive = true, isSensitiveInherited = false)
            val nonSensitivePhotoMock =
                createMockPhoto(isSensitive = false, isSensitiveInherited = false)

            setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = true)
            whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
                .thenReturn(elements)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    sensitivePhoto.nodeId,
                    sensitivePhoto,
                    false
                )
            )
                .thenReturn(sensitivePhotoMock)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nonSensitivePhoto.nodeId,
                    nonSensitivePhoto,
                    false
                )
            )
                .thenReturn(nonSensitivePhotoMock)

            initUseCase()
            val result = underTest(albumId, selectedCoverId = sensitivePhoto.id, refresh = false)

            // Should skip the sensitive photo and return the non-sensitive one
            assertThat(result).isEqualTo(nonSensitivePhotoMock)
        }

    @Test
    fun `test that sensitive cover is included for paid account with showHiddenItems true`() =
        runTest {
            val albumId = AlbumId(1L)
            val sensitivePhoto = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
            val elements = listOf(sensitivePhoto)

            val sensitivePhotoMock =
                createMockPhoto(isSensitive = true, isSensitiveInherited = false)

            setupDefaultHiddenItemsConfig(showHiddenItems = true, isPaid = true)
            whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
                .thenReturn(elements)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    sensitivePhoto.nodeId,
                    sensitivePhoto,
                    false
                )
            )
                .thenReturn(sensitivePhotoMock)

            initUseCase()
            val result = underTest(albumId, selectedCoverId = sensitivePhoto.id, refresh = false)

            // Should include the sensitive photo when paid and showHiddenItems = true
            assertThat(result).isEqualTo(sensitivePhotoMock)
        }

    @Test
    fun `test that null is returned when all photos are sensitive and filtered out`() = runTest {
        val albumId = AlbumId(1L)
        val sensitivePhoto1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val sensitivePhoto2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(sensitivePhoto1, sensitivePhoto2)

        val sensitivePhotoMock1 = createMockPhoto(isSensitive = true, isSensitiveInherited = false)
        val sensitivePhotoMock2 = createMockPhoto(isSensitive = false, isSensitiveInherited = true)

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = false)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                sensitivePhoto1.nodeId,
                sensitivePhoto1,
                false
            )
        )
            .thenReturn(sensitivePhotoMock1)
        whenever(
            photosRepository.getPhotoFromNodeID(
                sensitivePhoto2.nodeId,
                sensitivePhoto2,
                false
            )
        )
            .thenReturn(sensitivePhotoMock2)

        initUseCase()
        val result = underTest(albumId, selectedCoverId = null, refresh = false)

        // Should return null when all photos are sensitive
        assertThat(result).isNull()
    }

    @Test
    fun `test that fallback to next visible photo when selected cover is sensitive`() = runTest {
        val albumId = AlbumId(1L)
        val selectedCoverId = 30L
        val nonSensitivePhoto1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val sensitivePhoto = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val selectedSensitivePhoto = albumPhotoId(id = 30L, nodeId = 3000L, albumId = albumId.id)
        val elements = listOf(nonSensitivePhoto1, sensitivePhoto, selectedSensitivePhoto)

        val nonSensitivePhotoMock1 =
            createMockPhoto(isSensitive = false, isSensitiveInherited = false)
        val sensitivePhotoMock = createMockPhoto(isSensitive = true, isSensitiveInherited = false)
        val selectedSensitivePhotoMock =
            createMockPhoto(isSensitive = true, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = true)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                selectedSensitivePhoto.nodeId,
                selectedSensitivePhoto,
                false
            )
        )
            .thenReturn(selectedSensitivePhotoMock)
        whenever(photosRepository.getPhotoFromNodeID(sensitivePhoto.nodeId, sensitivePhoto, false))
            .thenReturn(sensitivePhotoMock)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nonSensitivePhoto1.nodeId,
                nonSensitivePhoto1,
                false
            )
        )
            .thenReturn(nonSensitivePhotoMock1)

        initUseCase()
        val result = underTest(albumId, selectedCoverId = selectedCoverId, refresh = false)

        // Should skip sensitive photos and return the first non-sensitive one from the end
        assertThat(result).isEqualTo(nonSensitivePhotoMock1)
    }

    private fun albumPhotoId(id: Long, nodeId: Long, albumId: Long): AlbumPhotoId =
        AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )

    private fun createMockPhoto(
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): Photo.Image = mock {
        on { this.isSensitive }.thenReturn(isSensitive)
        on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
    }
}


