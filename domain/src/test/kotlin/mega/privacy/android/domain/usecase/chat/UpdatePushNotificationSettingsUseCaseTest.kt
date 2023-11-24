package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdatePushNotificationSettingsUseCaseTest {
    private lateinit var underTest: UpdatePushNotificationSettingsUseCase

    private val notificationsRepository: NotificationsRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = UpdatePushNotificationSettingsUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            notificationsRepository,
        )
    }

    @Test
    fun `test that updatePushNotificationSettings is invoked`() = runTest {
        underTest()
        verify(notificationsRepository).updatePushNotificationSettings()
    }
}
