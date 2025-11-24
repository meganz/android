package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactLinkQueryFromLinkUseCaseTest {
    private lateinit var underTest: ContactLinkQueryFromLinkUseCase
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase = mock()
    private val contactLinkQueryUseCase: ContactLinkQueryUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = ContactLinkQueryFromLinkUseCase(
            getHandleFromContactLinkUseCase = getHandleFromContactLinkUseCase,
            contactLinkQueryUseCase = contactLinkQueryUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getHandleFromContactLinkUseCase,
            contactLinkQueryUseCase,
        )
    }

    @Test
    fun `test that get contact from link returns contact link`() = runTest {
        val handle = 123L
        val contactLinkQueryResult = mock<ContactLinkQueryResult> {

        }
        whenever(getHandleFromContactLinkUseCase("link")).thenReturn(handle)
        whenever(contactLinkQueryUseCase(handle)).thenReturn(contactLinkQueryResult)
        Truth.assertThat(underTest("link")).isEqualTo(contactLinkQueryResult)
    }

    @Test
    fun `test that get contact from link returns null`() = runTest {
        whenever(getHandleFromContactLinkUseCase("link")).thenReturn(-1L)
        Truth.assertThat(underTest("link")).isNull()
    }
}