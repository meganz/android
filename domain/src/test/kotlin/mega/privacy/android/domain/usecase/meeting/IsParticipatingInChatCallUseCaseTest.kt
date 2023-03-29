package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class IsParticipatingInChatCallUseCaseTest {
    private val repository = mock<CallRepository>()
    private val underTest = IsParticipatingInChatCallUseCase(repository)

    @Test
    fun `test use-case returns true when call handle list is not empty`() = runTest {
        whenever(repository.getCallHandleList(any())).thenReturn(listOf(123456))
        val actual = underTest()
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `test use-case returns false when call handle list is empty`() = runTest {
        whenever(repository.getCallHandleList(any())).thenReturn(emptyList())
        val actual = underTest()
        Truth.assertThat(actual).isFalse()
    }
}