package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AvatarRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAvatarFromBase64StringUseCaseTest {

    private lateinit var underTest: GetAvatarFromBase64StringUseCase

    private val repository = mock<AvatarRepository>()

    private val userHandle = 1234L
    private val base64String = "_9j_4AAQSkZJRgABAQAAAQABAAD_2wBDAAgG"

    @BeforeAll
    fun setup() {
        underTest = GetAvatarFromBase64StringUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that file is returned if repository returns a file`() = runTest {
        val file = mock<File>()

        whenever(repository.getAvatarFromBase64String(userHandle, base64String))
            .thenReturn(file)

        assertThat(underTest.invoke(userHandle, base64String)).isEqualTo(file)
    }

    @Test
    fun `test that null is returned if repository returns null`() = runTest {
        whenever(repository.getAvatarFromBase64String(userHandle, base64String))
            .thenReturn(null)

        assertThat(underTest.invoke(userHandle, base64String)).isNull()
    }
}