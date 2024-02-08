package mega.privacy.android.data.facade.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.chat.InMemoryChatDatabase
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatStorageFacadeTest {

    internal lateinit var underTest: ChatStorageFacade

    private val database = mock<InMemoryChatDatabase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = ChatStorageFacade(
            database,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            database,
        )
    }

    @ParameterizedTest(name = "pendingId = {0}")
    @ValueSource(longs = [-10, -1, 0])
    @NullSource
    fun `test that pending messages cannot be deleted when pendingMsgId is not a positive number`(
        pendingMessageId: Long?,
    ) = runTest {
        val pendingMessage = mock<PendingMessageEntity> {
            on { this.pendingMessageId } doReturn pendingMessageId
        }
        assertThrows<IllegalArgumentException> {
            underTest.deletePendingMessage(pendingMessage)
        }
        verifyNoInteractions(database)
    }
}