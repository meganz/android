package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AreAllContactParticipantsInChatUseCaseTest {
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase = mock()
    private lateinit var underTest: AreAllContactParticipantsInChatUseCase

    @BeforeAll
    fun setup() = runTest {
        underTest = AreAllContactParticipantsInChatUseCase(getVisibleContactsUseCase)
        val contacts = (1L..10L).map { handle ->
            mock<ContactItem> {
                on { this.handle } doReturn handle
            }
        }
        whenever(getVisibleContactsUseCase()).thenReturn(contacts)
    }

    @Test
    fun `test that returns true when all contacts are in the chat`() = runTest {
        Truth.assertThat(underTest(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L))).isTrue()
    }

    @Test
    fun `test that returns false when there is a contact not in the chat`() = runTest {
        Truth.assertThat(underTest(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L))).isFalse()
    }
}