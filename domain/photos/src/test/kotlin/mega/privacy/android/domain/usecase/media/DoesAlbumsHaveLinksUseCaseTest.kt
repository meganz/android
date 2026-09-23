package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesAlbumsHaveLinksUseCaseTest {

    private lateinit var underTest: DoesAlbumsHaveLinksUseCase

    private val albumRepository: AlbumRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = DoesAlbumsHaveLinksUseCase(
            albumRepository = albumRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(albumRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct value is returned`(haveLinks: Boolean) = runTest {
        whenever(albumRepository.haveLinks()) doReturn haveLinks

        val actual = underTest()

        assertThat(actual).isEqualTo(haveLinks)
    }
}
