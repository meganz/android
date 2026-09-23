package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetUserAlbumCoverPhotoUseCaseTest {

    private lateinit var underTest: GetUserAlbumCoverPhotoUseCase

    private val albumRepository: AlbumRepository = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            albumRepository,
            getNodeByIdUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase
        )
    }

    private fun initUseCase() {
        underTest = GetUserAlbumCoverPhotoUseCase(
            albumRepository = albumRepository,
            getNodeByIdUseCase = getNodeByIdUseCase,
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
    fun `test that refresh flag is forwarded to getAlbumElementIDs`() = runTest {
        val albumId = AlbumId(3L)
        val last = albumPhotoId(id = 30L, nodeId = 4000L, albumId = albumId.id)
        val expectedNode = createMockNode(id = 4000L)

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = true))
            .thenReturn(listOf(last))
        whenever(getNodeByIdUseCase(last.nodeId)).thenReturn(expectedNode)

        initUseCase()
        underTest(albumId, refresh = true)

        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = true)
    }

    @Test
    fun `test that cover node is returned when album has elements`() = runTest {
        val albumId = AlbumId(1L)
        val photoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val expectedNode = createMockNode(id = 100L, isMarkedSensitive = false, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(photoId))
        whenever(getNodeByIdUseCase(photoId.nodeId)).thenReturn(expectedNode)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(expectedNode)
        verify(albumRepository).getUserSet(albumId)
    }

    @Test
    fun `test that user set cover is used when available and visible`() = runTest {
        val albumId = AlbumId(1L)
        val coverPhotoId = albumPhotoId(id = 20L, nodeId = 200L, albumId = albumId.id)
        val otherPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val coverNode = createMockNode(id = 200L, modificationTime = 500L)
        val userSet = mock<UserSet> {
            on { cover }.thenReturn(20L)
        }

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(coverPhotoId, otherPhotoId))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(getNodeByIdUseCase(coverPhotoId.nodeId)).thenReturn(coverNode)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(coverNode)
        // Early return — the fallback should never fetch the other node
        verify(getNodeByIdUseCase, never()).invoke(otherPhotoId.nodeId)
    }

    @Test
    fun `test that fallback is used when selected cover node returns null`() = runTest {
        val albumId = AlbumId(1L)
        val coverPhotoId = albumPhotoId(id = 20L, nodeId = 200L, albumId = albumId.id)
        val otherPhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val otherNode = createMockNode(id = 100L, isMarkedSensitive = false, isSensitiveInherited = false)
        val userSet = mock<UserSet> {
            on { cover }.thenReturn(20L)
        }

        setupDefaultHiddenItemsConfig()
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(coverPhotoId, otherPhotoId))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(getNodeByIdUseCase(coverPhotoId.nodeId)).thenReturn(null)
        whenever(getNodeByIdUseCase(otherPhotoId.nodeId)).thenReturn(otherNode)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(otherNode)
    }

    @Test
    fun `test that free account includes all nodes regardless of sensitive status`() = runTest {
        val albumId = AlbumId(1L)
        val photoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val sensitiveNode = createMockNode(id = 100L, isMarkedSensitive = true, isSensitiveInherited = true)

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = false)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(photoId))
        whenever(getNodeByIdUseCase(photoId.nodeId)).thenReturn(sensitiveNode)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(sensitiveNode)
    }

    @Test
    fun `test that paid account with showHiddenItems false filters out sensitive nodes`() = runTest {
        val albumId = AlbumId(1L)
        val photoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val sensitiveNode = createMockNode(id = 100L, isMarkedSensitive = true, isSensitiveInherited = false)

        setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = true)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(photoId))
        whenever(getNodeByIdUseCase(photoId.nodeId)).thenReturn(sensitiveNode)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isNull()
    }

    @Test
    fun `test that paid account with showHiddenItems true includes sensitive nodes`() = runTest {
        val albumId = AlbumId(1L)
        val photoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
        val sensitiveNode = createMockNode(id = 100L, isMarkedSensitive = true, isSensitiveInherited = true)

        setupDefaultHiddenItemsConfig(showHiddenItems = true, isPaid = true)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(listOf(photoId))
        whenever(getNodeByIdUseCase(photoId.nodeId)).thenReturn(sensitiveNode)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isEqualTo(sensitiveNode)
    }

    @Test
    fun `test that most recently modified non-sensitive node is used when user set cover is sensitive and hidden items disabled`() =
        runTest {
            val albumId = AlbumId(1L)
            val sensitivePhotoId = albumPhotoId(id = 20L, nodeId = 200L, albumId = albumId.id)
            val nonSensitivePhotoId = albumPhotoId(id = 10L, nodeId = 100L, albumId = albumId.id)
            val sensitiveNode = createMockNode(
                id = 200L,
                modificationTime = 1000L,
                isMarkedSensitive = true,
                isSensitiveInherited = false
            )
            val nonSensitiveNode = createMockNode(
                id = 100L,
                modificationTime = 500L,
                isMarkedSensitive = false,
                isSensitiveInherited = false
            )
            val userSet = mock<UserSet> {
                on { cover }.thenReturn(20L)
            }

            setupDefaultHiddenItemsConfig(showHiddenItems = false, isPaid = true)
            whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
                .thenReturn(listOf(sensitivePhotoId, nonSensitivePhotoId))
            whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
            whenever(getNodeByIdUseCase(sensitivePhotoId.nodeId)).thenReturn(sensitiveNode)
            whenever(getNodeByIdUseCase(nonSensitivePhotoId.nodeId)).thenReturn(nonSensitiveNode)

            initUseCase()
            val result = underTest(albumId)

            assertThat(result).isEqualTo(nonSensitiveNode)
        }

    private fun albumPhotoId(id: Long, nodeId: Long, albumId: Long): AlbumPhotoId =
        AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )

    private fun createMockNode(
        id: Long = 0L,
        modificationTime: Long = 0L,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): TypedFileNode = mock {
        on { this.id }.thenReturn(NodeId(id))
        on { this.modificationTime }.thenReturn(modificationTime)
        on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
        on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
    }
}
