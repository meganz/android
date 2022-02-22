package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.*
import mega.privacy.android.app.domain.repository.ChatRepository
import mega.privacy.android.app.domain.usecase.DefaultIsOnCall
import mega.privacy.android.app.domain.usecase.IsOnCall
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DefaultIsOnCallTest {
    private lateinit var underTest: IsOnCall

    private val chatRepository = mock<ChatRepository>()

    @Before
    fun setUp() {
        runBlocking {
            whenever(chatRepository.getCallCountByState(any())).thenReturn(0)
        }
        underTest = DefaultIsOnCall(chatRepository = chatRepository)
    }

    @After
    fun tearDown() {
        Mockito.reset(chatRepository)
    }

    @Test
    fun `test that empty list returns false`() = runTest{
        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test initial call returns true`() = runTest{
        whenever(chatRepository.getCallCountByState(CallStatus.Initial)).thenReturn(1)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test connecting call returns true`() = runTest{
        whenever(chatRepository.getCallCountByState(CallStatus.Connecting)).thenReturn(1)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test joining call returns true`() = runTest{
        whenever(chatRepository.getCallCountByState(CallStatus.Joining)).thenReturn(1)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test in progress call returns true`() = runTest{
        whenever(chatRepository.getCallCountByState(CallStatus.InProgress)).thenReturn(1)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that list with no valid state returns false`() = runTest{
        whenever(chatRepository.getCallCountByState(CallStatus.Destroyed)).thenReturn(1)

        assertThat(underTest()).isFalse()
    }


}