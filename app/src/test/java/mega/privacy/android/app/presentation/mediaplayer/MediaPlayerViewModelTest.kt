package mega.privacy.android.app.presentation.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodeDataUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import mega.privacy.android.shared.resources.R as sharedResR

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaPlayerViewModelTest {
    private lateinit var underTest: MediaPlayerViewModel

    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val checkChatNodesNameCollisionAndCopyUseCase =
        mock<CheckChatNodesNameCollisionAndCopyUseCase>()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getPublicAlbumNodeDataUseCase = mock<GetPublicAlbumNodeDataUseCase>()
    private val getFileUriUseCase = mock<GetFileUriUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val fakeMonitorShowHiddenItemsFlow = MutableSharedFlow<Boolean>()
    private val fakeMonitorAccountDetailFlow = MutableSharedFlow<AccountDetail>()

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(
            fakeMonitorShowHiddenItemsFlow
        )
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(fakeMonitorAccountDetailFlow)
        wheneverBlocking { isHiddenNodesOnboardedUseCase() }.thenReturn(false)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = MediaPlayerViewModel(
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase = checkChatNodesNameCollisionAndCopyUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            getChatFileUseCase = getChatFileUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getPublicAlbumNodeDataUseCase = getPublicAlbumNodeDataUseCase,
            getFileUriUseCase = getFileUriUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase,
            getFileUriUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
        )
    }

    @Test
    internal fun `test that copy complete snack bar message is shown when node is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackbarMessage().test().assertValue(R.string.context_correctly_copied)
        }

    @Test
    internal fun `test that copy error snack bar message is shown when node is not copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackbarMessage().test().assertValue(R.string.context_no_copied)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ).thenThrow(runtimeException)
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test move complete snack bar message is shown when node is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(sharedResR.string.node_moved_success_message)
            }
        }

    @Test
    internal fun `test move error snack bar message is shown when node is not moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_moved)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ).thenThrow(runtimeException)

            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test that copy complete snack bar message is shown when chat node is imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test that copy error snack bar message is shown when chat node is not imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L

            val runtimeException = RuntimeException("Import node failed")
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ).thenThrow(runtimeException)

            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().observeOnce {
                assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    fun `test that the metadata is updated correctly`() = runTest {
        val metadata = Metadata(
            title = "Title",
            artist = "Artist",
            album = "Album",
            nodeName = "Node Name"
        )
        underTest.metadataState.test {
            val initial = awaitItem()
            assertThat(initial.title).isNull()
            assertThat(initial.artist).isNull()
            assertThat(initial.album).isNull()
            assertThat(initial.nodeName).isEmpty()
            underTest.updateMetaData(metadata)
            val actual = awaitItem()
            assertThat(actual.title).isEqualTo(metadata.title)
            assertThat(actual.artist).isEqualTo(metadata.artist)
            assertThat(actual.album).isEqualTo(metadata.album)
            assertThat(actual.nodeName).isEqualTo(metadata.nodeName)
        }
    }

    @Test
    internal fun `test that snackbar message is shown when chat file is already available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(true)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onSnackbarMessage().test().assertValue(R.string.file_already_exists)
        }

    @Test
    internal fun `test that startChatFileOfflineDownload event is triggered when chat file is not available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(false)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onStartChatFileOfflineDownload().test().assertValue(chatFile)
        }

    @Test
    internal fun `test that exception is handled correctly when chat file is not found`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(null)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue {
                it is IllegalStateException
            }
        }

    @Test
    fun `test that getFileUriUseCase returns as expected`() = runTest {
        val expectedUri = "content://test/12345"
        whenever(getFileUriUseCase(any(), any())).thenReturn(expectedUri)

        val actual = underTest.getContentUri(mock())
        assertThat(actual).isEqualTo(expectedUri)
    }

    @Test
    fun `test that handleHiddenNodesUIFlow updates state correctly for paid account`() = runTest {
        val testAccountType = mock<AccountType> {
            on { isBusinessAccount }.thenReturn(true)
            on { isPaid }.thenReturn(true)
        }
        initUnderTest()
        advanceUntilIdle()

        underTest.state.test {
            fakeMonitorShowHiddenItemsFlow.emit(true)
            emitAccountDetail(testAccountType)
            skipItems(1)
            val actual = awaitItem()
            assertThat(actual.accountType?.isPaid).isTrue()
            assertThat(actual.isBusinessAccountExpired).isFalse()
            assertThat(actual.showHiddenItems).isTrue()
            cancelAndConsumeRemainingEvents()
        }
    }

    private suspend fun emitAccountDetail(accountType: AccountType? = null) {
        val testLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType }.thenReturn(accountType)
        }
        val testAccountDetail = mock<AccountDetail> {
            on { levelDetail }.thenReturn(testLevelDetail)
        }
        fakeMonitorAccountDetailFlow.emit(testAccountDetail)
    }

    @Test
    fun `test that handleHiddenNodesUIFlow updates state correctly for business account`() =
        runTest {
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

            val testAccountType = mock<AccountType> {
                on { isBusinessAccount }.thenReturn(true)
                on { isPaid }.thenReturn(true)
            }
            initUnderTest()
            advanceUntilIdle()

            underTest.state.test {
                emitAccountDetail(testAccountType)
                fakeMonitorShowHiddenItemsFlow.emit(false)
                skipItems(1)
                val actual = awaitItem()
                assertThat(actual.accountType?.isPaid).isTrue()
                assertThat(actual.isBusinessAccountExpired).isTrue()
                assertThat(actual.showHiddenItems).isFalse()
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that handleHiddenNodesUIFlow updates state correctly for free account`() = runTest {
        val testAccountType = mock<AccountType> {
            on { isBusinessAccount }.thenReturn(true)
            on { isPaid }.thenReturn(false)
        }
        initUnderTest()
        advanceUntilIdle()

        underTest.state.test {
            emitAccountDetail(testAccountType)
            fakeMonitorShowHiddenItemsFlow.emit(true)
            skipItems(1)
            val actual = awaitItem()
            assertThat(actual.accountType?.isPaid).isFalse()
            assertThat(actual.isBusinessAccountExpired).isFalse()
            assertThat(actual.showHiddenItems).isTrue()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that handleHiddenNodesUIFlow handles null account detail gracefully`() = runTest {
        initUnderTest()
        advanceUntilIdle()

        underTest.state.test {
            fakeMonitorAccountDetailFlow.emit(mock())
            fakeMonitorShowHiddenItemsFlow.emit(false)
            skipItems(1)
            val actual = awaitItem()
            assertThat(actual.accountType).isNull()
            assertThat(actual.isBusinessAccountExpired).isFalse()
            assertThat(actual.showHiddenItems).isFalse()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that handleHiddenNodesUIFlow handles null account level detail gracefully`() =
        runTest {
            val mockAccountDetail = mock<AccountDetail> {
                on { levelDetail }.thenReturn(null)
            }
            initUnderTest()
            advanceUntilIdle()

            underTest.state.test {
                fakeMonitorAccountDetailFlow.emit(mockAccountDetail)
                fakeMonitorShowHiddenItemsFlow.emit(true)
                skipItems(1)
                val actual = awaitItem()
                assertThat(actual.accountType).isNull()
                assertThat(actual.isBusinessAccountExpired).isFalse()
                assertThat(actual.showHiddenItems).isTrue()
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that handleHiddenNodesUIFlow handles null account type gracefully`() = runTest {
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        initUnderTest()
        advanceUntilIdle()

        underTest.state.test {
            emitAccountDetail()
            fakeMonitorShowHiddenItemsFlow.emit(false)
            skipItems(1)
            val actual = awaitItem()
            assertThat(actual.accountType).isNull()
            assertThat(actual.isBusinessAccountExpired).isFalse()
            assertThat(actual.showHiddenItems).isFalse()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that handleHiddenNodesUIFlow handles business status for non-business account`() =
        runTest {
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            val testAccountType = mock<AccountType> {
                on { isPaid }.thenReturn(true)
                on { isBusinessAccount }.thenReturn(false)
            }
            initUnderTest()
            advanceUntilIdle()

            underTest.state.test {
                emitAccountDetail(testAccountType)
                fakeMonitorShowHiddenItemsFlow.emit(true)
                skipItems(1)
                val actual = awaitItem()
                assertThat(actual.accountType?.isPaid).isTrue()
                assertThat(actual.isBusinessAccountExpired).isFalse()
                assertThat(actual.showHiddenItems).isTrue()
                cancelAndConsumeRemainingEvents()
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}