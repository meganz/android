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
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

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
    fun `test that refresh flag is forwarded to both repositories`() = runTest {
        val albumId = AlbumId(3L)
        val last = albumPhotoId(id = 30L, nodeId = 4000L, albumId = albumId.id)
        val expectedPhoto =
            createMockPhoto(id = 30L, isSensitive = false, isSensitiveInherited = false)

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
        underTest(albumId, refresh = true)

        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = true)
        verify(photosRepository).getPhotoFromNodeID(last.nodeId, last, true)
    }

    @Test
    fun `test that cover photo is returned when album has elements`() = runTest {
        val albumId = AlbumId(1L)
        val albumPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val expectedPhoto =
            createMockPhoto(id = 10L, isSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(albumPhotoId))
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = albumPhotoId.nodeId,
                albumPhotoId = albumPhotoId,
                refresh = false
            )
        ).thenReturn(expectedPhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(expectedPhoto)
        verify(albumRepository).getUserSet(albumId)
    }

    @Test
    fun `test that user set cover is used when available and visible`() = runTest {
        val albumId = AlbumId(1L)
        val coverPhotoId = albumPhotoId(id = 20L, nodeId = 200L, albumId = albumId.id)
        val otherPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val coverPhoto =
            createMockPhoto(id = 20L, modificationTime = LocalDateTime.now().minusDays(1))
        val otherPhoto = createMockPhoto(id = 10L, modificationTime = LocalDateTime.now())

        val userSet = mock<UserSet> {
            on { cover }.thenReturn(20L)
        }

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(coverPhotoId, otherPhotoId))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = coverPhotoId.nodeId,
                albumPhotoId = coverPhotoId,
                refresh = false
            )
        ).thenReturn(coverPhoto)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = otherPhotoId.nodeId,
                albumPhotoId = otherPhotoId,
                refresh = false
            )
        ).thenReturn(otherPhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(coverPhoto)
        // Early return — the fallback mapNotNull should never fetch the other photo
        verify(photosRepository, never()).getPhotoFromNodeID(
            nodeId = otherPhotoId.nodeId,
            albumPhotoId = otherPhotoId,
            refresh = false,
        )
    }

    @Test
    fun `test that fallback is used when selected cover node returns null from repository`() =
        runTest {
            val albumId = AlbumId(1L)
            val coverPhotoId = albumPhotoId(id = 20L, nodeId = 200L, albumId = albumId.id)
            val otherPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
            val otherPhoto = createMockPhoto(
                id = 10L,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val userSet = mock<UserSet> {
                on { cover }.thenReturn(20L)
            }

            setupDefaultHiddenItemsConfig()
            whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
                .thenReturn(listOf(coverPhotoId, otherPhotoId))
            whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = coverPhotoId.nodeId,
                    albumPhotoId = coverPhotoId,
                    refresh = false,
                )
            ).thenReturn(null)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = otherPhotoId.nodeId,
                    albumPhotoId = otherPhotoId,
                    refresh = false,
                )
            ).thenReturn(otherPhoto)

            initUseCase()
            val result = underTest(albumId)

            assertThat(result).isEqualTo(otherPhoto)
        }

    @Test
    fun `test that free account includes all photos regardless of sensitive status`() = runTest {
        val albumId = AlbumId(1L)
        val albumPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val sensitivePhoto = createMockPhoto(
            id = 10L,
            isSensitive = true,
            isSensitiveInherited = true
        )

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = false)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(albumPhotoId))
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = albumPhotoId.nodeId,
                albumPhotoId = albumPhotoId,
                refresh = false
            )
        ).thenReturn(sensitivePhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(sensitivePhoto)
    }

    @Test
    fun `test that paid account with showHiddenItems false filters out sensitive photos`() =
        runTest {
            val albumId = AlbumId(1L)
            val albumPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
            val sensitivePhoto = createMockPhoto(
                id = 10L,
                isSensitive = true,
                isSensitiveInherited = false
            )

            setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = true)
            whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
                .thenReturn(listOf(albumPhotoId))
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = albumPhotoId.nodeId,
                    albumPhotoId = albumPhotoId,
                    refresh = false
                )
            ).thenReturn(sensitivePhoto)

            initUseCase()
            val result = underTest(albumId)

            assertThat(result).isNull()
        }

    @Test
    fun `test that paid account with showHiddenItems true includes sensitive photos`() = runTest {
        val albumId = AlbumId(1L)
        val albumPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val sensitivePhoto = createMockPhoto(
            id = 10L,
            isSensitive = true,
            isSensitiveInherited = true
        )

        setupDefaultHiddenItemsConfig(showHiddenItems = true, isPaid = true)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(albumPhotoId))
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = albumPhotoId.nodeId,
                albumPhotoId = albumPhotoId,
                refresh = false
            )
        ).thenReturn(sensitivePhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(sensitivePhoto)
    }

    @Test
    fun `test that most recently modified non-sensitive photo is used when user set cover is sensitive and hidden items disabled`() =
        runTest {
            val albumId = AlbumId(1L)
            val sensitivePhotoId = albumPhotoId(id = 20L, nodeId = 200L, albumId = albumId.id)
            val nonSensitivePhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
            val sensitivePhoto = createMockPhoto(
                id = 20L,
                modificationTime = LocalDateTime.now(),
                isSensitive = true,
                isSensitiveInherited = false
            )
            val nonSensitivePhoto = createMockPhoto(
                id = 10L,
                modificationTime = LocalDateTime.now().minusDays(1),
                isSensitive = false,
                isSensitiveInherited = false
            )

            val userSet = mock<UserSet> {
                on { cover }.thenReturn(20L)
            }

            setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = true)
            whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
                .thenReturn(listOf(sensitivePhotoId, nonSensitivePhotoId))
            whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = sensitivePhotoId.nodeId,
                    albumPhotoId = sensitivePhotoId,
                    refresh = false
                )
            ).thenReturn(sensitivePhoto)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = nonSensitivePhotoId.nodeId,
                    albumPhotoId = nonSensitivePhotoId,
                    refresh = false
                )
            ).thenReturn(nonSensitivePhoto)

            initUseCase()
            val result = underTest(albumId)

            assertThat(result).isEqualTo(nonSensitivePhoto)
        }

    private fun albumPhotoId(id: Long, nodeId: Long, albumId: Long): AlbumPhotoId =
        AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )

    private fun createMockPhoto(
        id: Long = 0L,
        modificationTime: LocalDateTime = LocalDateTime.now(),
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): Photo.Image = mock {
        on { this.id }.thenReturn(id)
        on { this.modificationTime }.thenReturn(modificationTime)
        on { this.isSensitive }.thenReturn(isSensitive)
        on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
    }
}


