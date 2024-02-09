package mega.privacy.android.data.facade.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.chat.InMemoryChatDatabase
import mega.privacy.android.data.database.dao.PendingMessageDao
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
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

    @Test
    fun `test that pending messages are not deleted when the id is null`() = runTest {
        val pendingMessage = mock<PendingMessageEntity> {
            on { this.pendingMessageId } doReturn null
        }
        val pendingMessageDao = mock<PendingMessageDao>()
        database.stub { on { pendingMessageDao() } doReturn pendingMessageDao }

        underTest.deletePendingMessage(pendingMessage)
        verifyNoInteractions(pendingMessageDao)
    }
}