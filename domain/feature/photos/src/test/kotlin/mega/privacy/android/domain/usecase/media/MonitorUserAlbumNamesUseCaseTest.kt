package mega.privacy.android.domain.usecase.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorUserAlbumNamesUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorUserAlbumNamesUseCaseTest {
    private lateinit var underTest: MonitorUserAlbumNamesUseCase

    private val albumRepository: AlbumRepository = mock()

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        reset(albumRepository)
    }

    private fun initUseCase() {
        underTest = MonitorUserAlbumNamesUseCase(
            albumRepository = albumRepository,
            defaultDispatcher = testDispatcher,
        )
    }

    @Test
    fun `test that the album names are emitted on start`() = runTest {
        val userSets = createMockUserSets()
        whenever(albumRepository.getAllUserSets()).thenReturn(userSets)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(emptyFlow())

        initUseCase()

        underTest().test {
            assertThat(awaitItem()).containsExactly("Album 1", "Album 2", "Album 3").inOrder()
            awaitComplete()
        }
    }

    @Test
    fun `test that the album names are re-emitted when user sets change`() = runTest {
        val userSets = createMockUserSets()
        whenever(albumRepository.getAllUserSets()).thenReturn(userSets)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(userSets))

        initUseCase()

        underTest().test {
            // onStart emission
            assertThat(awaitItem()).containsExactly("Album 1", "Album 2", "Album 3").inOrder()
            // monitorUserSetsUpdate emission
            assertThat(awaitItem()).containsExactly("Album 1", "Album 2", "Album 3").inOrder()
            awaitComplete()
        }
    }

    @Test
    fun `test that an empty list is emitted when there are no user sets`() = runTest {
        whenever(albumRepository.getAllUserSets()).thenReturn(emptyList())
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(emptyFlow())

        initUseCase()

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    private fun createMockUserSets(): List<UserSet> = listOf(
        mock<UserSet> {
            on { id }.thenReturn(1L)
            on { name }.thenReturn("Album 1")
        },
        mock<UserSet> {
            on { id }.thenReturn(2L)
            on { name }.thenReturn("Album 2")
        },
        mock<UserSet> {
            on { id }.thenReturn(3L)
            on { name }.thenReturn("Album 3")
        },
    )
}
