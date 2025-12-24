package mega.privacy.android.domain.usecase.node.publiclink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DoesHaveLinksUseCaseTest {

    private lateinit var underTest: DoesHaveLinksUseCase
    private val shareRepository = mock<ShareRepository>()

    @BeforeEach
    fun setUp() {
        underTest = DoesHaveLinksUseCase(
            shareRepository = shareRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(shareRepository)
    }

    @Test
    fun `test that invoke returns true when account has public links`() = runTest {
        shareRepository.stub {
            onBlocking { doesHaveLinks() }.thenReturn(true)
        }

        val result = underTest()

        assertThat(result).isTrue()
        verify(shareRepository).doesHaveLinks()
    }

    @Test
    fun `test that invoke returns false when account has no public links`() = runTest {
        shareRepository.stub {
            onBlocking { doesHaveLinks() }.thenReturn(false)
        }

        val result = underTest()

        assertThat(result).isFalse()
        verify(shareRepository).doesHaveLinks()
    }

    @Test
    fun `test that invoke calls repository doesHaveLinks method`() = runTest {
        shareRepository.stub {
            onBlocking { doesHaveLinks() }.thenReturn(true)
        }

        underTest()

        verify(shareRepository).doesHaveLinks()
    }
}
