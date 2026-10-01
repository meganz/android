package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAlbumLinkValidUseCaseTest {
    private lateinit var underTest: IsAlbumLinkValidUseCase

    private val albumRepository: AlbumRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = IsAlbumLinkValidUseCase(
            albumRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(albumRepository)
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test that use case returns correct result`(
        domain: String,
    ) = runTest {
        // given
        val albumLink = AlbumLink("https://$domain/collection")

        whenever(albumRepository.isAlbumLinkValid(albumLink))
            .thenReturn(true)

        // when
        val isValid = underTest(albumLink)

        // then
        assertThat(isValid).isTrue()
    }

    private fun domainProvider() = listOf("mega.nz", "mega.app")
}
