package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorChatOnlineStatusUseCaseTest {
    private val contactsRepository = mock<ContactsRepository>()
    private val underTest = MonitorChatOnlineStatusUseCase(contactsRepository)

    @Test
    fun `test that monitor last green updates returns flow of user last green`() = runTest {
        whenever(contactsRepository.monitorChatOnlineStatusUpdates()).thenReturn(
            flowOf(
                OnlineStatus(userHandle = 123456L, status = UserStatus.Away, inProgress = false)
            )
        )

        underTest().test {
            val actual = awaitItem()
            awaitComplete()
            Truth.assertThat(actual.status).isEqualTo(UserStatus.Away)
        }
    }
}