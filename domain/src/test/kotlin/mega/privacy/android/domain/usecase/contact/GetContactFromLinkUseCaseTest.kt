package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetContactFromLinkUseCaseTest {
    private lateinit var underTest: GetContactFromLinkUseCase
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase = mock()
    private val getContactLinkUseCase: GetContactLinkUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = GetContactFromLinkUseCase(
            getHandleFromContactLinkUseCase = getHandleFromContactLinkUseCase,
            getContactLinkUseCase = getContactLinkUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getHandleFromContactLinkUseCase,
            getContactLinkUseCase,
        )
    }

    @Test
    fun `test that get contact from link returns contact link`() = runTest {
        val handle = 123L
        val contactLink = mock<ContactLink> {

        }
        whenever(getHandleFromContactLinkUseCase("link")).thenReturn(handle)
        whenever(getContactLinkUseCase(handle)).thenReturn(contactLink)
        Truth.assertThat(underTest("link")).isEqualTo(contactLink)
    }

    @Test
    fun `test that get contact from link returns null`() = runTest {
        whenever(getHandleFromContactLinkUseCase("link")).thenReturn(-1L)
        Truth.assertThat(underTest("link")).isNull()
    }
}