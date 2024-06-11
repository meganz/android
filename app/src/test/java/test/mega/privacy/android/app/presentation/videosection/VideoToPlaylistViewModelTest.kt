package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistViewModel
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistSetsUseCase
import nz.mega.sdk.MegaSet
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoToPlaylistViewModelTest {
    private lateinit var underTest: VideoToPlaylistViewModel

    private val getVideoPlaylistSetsUseCase = mock<GetVideoPlaylistSetsUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { whenever(getVideoPlaylistSetsUseCase()).thenReturn(emptyList()) }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoToPlaylistViewModel(
            getVideoPlaylistSetsUseCase = getVideoPlaylistSetsUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getVideoPlaylistSetsUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the state is updated correctly when getVideoPlaylistSetsUseCase returns empty`() =
        runTest {
            whenever(getVideoPlaylistSetsUseCase()).thenReturn(emptyList())
            initUnderTest()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state is updated correctly when when getVideoPlaylistSetsUseCase returns not empty`() =
        runTest {
            val expectedUserSets = (1..3L).map {
                createUserSet(id = it, name = "Playlist $it")
            }
            whenever(getVideoPlaylistSetsUseCase()).thenReturn(expectedUserSets)

            initUnderTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items).isNotEmpty()
                assertThat(actual.items.size).isEqualTo(3)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createUserSet(
        id: Long,
        name: String,
    ): UserSet = object : UserSet {
        override val id: Long = id
        override val name: String = name
        override val type: Int = MegaSet.SET_TYPE_PLAYLIST
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
