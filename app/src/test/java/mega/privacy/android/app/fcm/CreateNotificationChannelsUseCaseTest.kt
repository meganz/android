package mega.privacy.android.app.fcm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateNotificationChannelsUseCaseTest {
    private lateinit var underTest: CreateNotificationChannelsUseCase

    private val createChatNotificationChannelsUseCase =
        mock<CreateChatNotificationChannelsUseCase>()
    private val createTransferNotificationChannelsUseCase =
        mock<CreateTransferNotificationChannelsUseCase>()

    @BeforeAll
    fun init() {
        underTest = CreateNotificationChannelsUseCase(
            createChatNotificationChannelsUseCase = createChatNotificationChannelsUseCase,
            createTransferNotificationChannelsUseCase = createTransferNotificationChannelsUseCase,
        )

    }

    @BeforeEach
    fun resetMocks() {
        reset(createChatNotificationChannelsUseCase, createTransferNotificationChannelsUseCase)
    }

    @Test
    fun `test that create chat notification channels are invoked when the use case is invoked`() =
        runTest {
            underTest()
            verify(createChatNotificationChannelsUseCase).invoke()
        }

    @Test
    fun `test that create transfer notification channels are invoked when the use case is invoked`() =
        runTest {
            underTest()
            verify(createTransferNotificationChannelsUseCase).invoke()
        }
}