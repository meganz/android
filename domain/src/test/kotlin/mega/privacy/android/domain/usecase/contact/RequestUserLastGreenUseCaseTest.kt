package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestUserLastGreenUseCaseTest {

    private lateinit var underTest: RequestUserLastGreenUseCase

    private val contactsRepository = mock<ContactsRepository>()

    @BeforeEach
    fun setup() {
        underTest = RequestUserLastGreenUseCase(contactsRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(contactsRepository)
    }

    @Test
    fun `test that contact repository invokes request last green when use case is invoked`() =
        runTest {
            val userHandle = 123L
            underTest.invoke(userHandle)
            verify(contactsRepository).requestLastGreen(userHandle)
            verifyNoMoreInteractions(contactsRepository)
        }
}