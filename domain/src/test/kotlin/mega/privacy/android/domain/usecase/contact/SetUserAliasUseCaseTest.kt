package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SetUserAliasUseCaseTest {
    private val contactsRepository = mock<ContactsRepository>()
    private val underTest = SetUserAliasUseCase(contactsRepository)
    private val testHandle = 12345L
    private val aliasName = "Alias"

    @Test
    fun `test that use case returns alias if alias name is updated`() = runTest {
        whenever(contactsRepository.setUserAlias(aliasName, testHandle)).thenReturn(aliasName)
        val actual = underTest(aliasName, testHandle)
        Truth.assertThat(actual).isEqualTo(aliasName)
    }

    @Test
    fun `test that use case returns null if alias name is cleared^`() = runTest {
        whenever(contactsRepository.setUserAlias(null, testHandle)).thenReturn(null)
        val actual = underTest(null, testHandle)
        Truth.assertThat(actual).isNull()
    }
}