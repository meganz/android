package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DoesUriPathHaveSufficientSpaceUseCaseTest {
    private lateinit var underTest: DoesUriPathHaveSufficientSpaceUseCase

    private val getDiskSpaceBytesUseCase = mock<GetDiskSpaceBytesUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DoesUriPathHaveSufficientSpaceUseCase(
            getDiskSpaceBytesUseCase = getDiskSpaceBytesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getDiskSpaceBytesUseCase)
    }

    @Test
    internal fun `test that false is returned if the path has less space than required, `() =
        runTest {
            val required = 123L
            getDiskSpaceBytesUseCase.stub { onBlocking { invoke(any()) }.thenReturn(required - 1) }

            assertThat(underTest(UriPath(""), required)).isFalse()
        }

    @Test
    internal fun `test that true is returned if the path has more space than required`() =
        runTest {
            val required = 123L
            getDiskSpaceBytesUseCase.stub { onBlocking { invoke(any()) }.thenReturn(required + 1) }

            assertThat(underTest(UriPath(""), required)).isTrue()
        }

    @Test
    internal fun `test that false is returned if file repository throws an error`() = runTest {
        val required = 123L
        getDiskSpaceBytesUseCase.stub { onBlocking { invoke(any()) }.thenAnswer { throw IllegalArgumentException() } }

        assertThat(underTest(UriPath(""), required)).isFalse()
    }
}