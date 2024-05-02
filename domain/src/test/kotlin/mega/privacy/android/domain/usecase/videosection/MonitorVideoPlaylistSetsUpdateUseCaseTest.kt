package mega.privacy.android.domain.usecase.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorVideoPlaylistSetsUpdateUseCaseTest {
    private lateinit var underTest: MonitorVideoPlaylistSetsUpdateUseCase

    private val videoSectionRepository = mock<VideoSectionRepository>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = MonitorVideoPlaylistSetsUpdateUseCase(
            videoSectionRepository = videoSectionRepository,
            nodeRepository = nodeRepository,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(videoSectionRepository, nodeRepository)
    }

    @Test
    fun `test that invoke returns when sets is updated`() = runTest {
        val expectedResult = flowOf(listOf(1L, 2L, 3L))
        whenever(videoSectionRepository.monitorSetsUpdates()).thenReturn(expectedResult)
        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(listOf(1L, 2L, 3L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke returns when the item of the video playlist is updated`() = runTest {
        val expectedResult = listOf(1L)
        val expectedVideoPlaylistMap = mapOf(1L to createUserSet())
        val set = mutableSetOf(1L)
        val expectedVideoSetsMap = mapOf(NodeId(1L) to set)
        val node = mock<Node> {
            on { id }.thenReturn(NodeId(1L))
        }
        val nodeUpdate = mock<NodeUpdate> {
            on { changes }.thenReturn(
                mapOf(
                    node to emptyList()
                )
            )
        }

        whenever(videoSectionRepository.getVideoSetsMap()).thenReturn(expectedVideoSetsMap)
        whenever(videoSectionRepository.getVideoPlaylistsMap()).thenReturn(expectedVideoPlaylistMap)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(nodeUpdate))
        whenever(nodeRepository.monitorOfflineNodeUpdates()).thenReturn(emptyFlow())
        whenever(videoSectionRepository.monitorSetsUpdates()).thenReturn(emptyFlow())
        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(expectedResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke returns when the item of offline node is updated`() = runTest {
        val expectedResult = listOf(1L)
        val expectedOffline = mock<Offline> {
            on { handle }.thenReturn(1.toString())
        }
        val expectedOfflineList = flowOf(listOf(expectedOffline))

        whenever(nodeRepository.monitorOfflineNodeUpdates()).thenReturn(expectedOfflineList)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(emptyFlow())
        whenever(videoSectionRepository.monitorSetsUpdates()).thenReturn(emptyFlow())
        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(expectedResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createUserSet(): UserSet = object : UserSet {
        override val id: Long = 1

        override val name: String = "name"

        override val type: Int = 1

        override val cover: Long? = null

        override val creationTime: Long = 0

        override val modificationTime: Long = 0

        override val isExported: Boolean = false

        override fun equals(other: Any?): Boolean {
            val otherSet = other as? UserSet ?: return false
            return id == otherSet.id
                    && name == otherSet.name
                    && cover == otherSet.cover
                    && modificationTime == otherSet.modificationTime
                    && isExported == otherSet.isExported
        }
    }
}