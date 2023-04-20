package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorLastGreenUpdatesUseCaseTest {

    private val contactsRepository = mock<ContactsRepository>()
    private val underTest = MonitorLastGreenUpdatesUseCase(contactsRepository)

    @Test
    fun `test that monitor last green updates returns flow of user last green`() = runTest {
        whenever(contactsRepository.monitorChatPresenceLastGreenUpdates()).thenReturn(
            flowOf(
                UserLastGreen(handle = 123456L, lastGreen = 5)
            )
        )

        underTest().test {
            val actual = awaitItem()
            awaitComplete()
            Truth.assertThat(actual.lastGreen).isEqualTo(5)
        }
    }
}