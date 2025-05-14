package mega.privacy.android.app.presentation.filecontact

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactInfo
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.triggeredContent
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.foldernode.ShareFolderUseCase
import mega.privacy.android.domain.usecase.shares.GetAllowedSharingPermissionsUseCase
import mega.privacy.android.domain.usecase.shares.MonitorShareRecipientsUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ShareRecipientsViewModelTest {
    private lateinit var underTest: ShareRecipientsViewModel

    private val monitorShareRecipientsUseCase = mock<MonitorShareRecipientsUseCase>()
    private val shareFolderUseCase = mock<ShareFolderUseCase>()
    private val shareFolderRequestMapper = mock<MoveRequestMessageMapper>()
    private val getAllowedSharingPermissionsUseCase = mock<GetAllowedSharingPermissionsUseCase>()
    private val getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>()

    private val shareResultMapper = RemoveShareResultMapper(
        successString = { TestValues.SUCCESS_STRING },
        errorString = { TestValues.FAILURE_STRING }
    )

    @Before
    fun setUp() {
        reset(
            monitorShareRecipientsUseCase,
            shareFolderUseCase,
            getAllowedSharingPermissionsUseCase,
            getContactVerificationWarningUseCase,
        )
    }

    private fun initUnderTest() {
        underTest = ShareRecipientsViewModel(
            savedStateHandle = SavedStateHandle.Companion.invoke(
                route = FileContactInfo(
                    folderName = TestValues.FOLDER_NAME,
                    folderHandle = TestValues.NODE_HANDLE,
                ),
            ),
            monitorShareRecipientsUseCase = monitorShareRecipientsUseCase,
            shareFolderUseCase = shareFolderUseCase,
            removeShareResultMapper = shareResultMapper,
            moveRequestMessageMapper = shareFolderRequestMapper,
            getAllowedSharingPermissionsUseCase = getAllowedSharingPermissionsUseCase,
            getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        )
    }

    @Test
    fun `test that loading state is emitted while loading`() = runTest {
        monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
        initUnderTest()
        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(FileContactListState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that data state is emitted once use case returns`() = runTest {
        getAllowedSharingPermissionsUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(setOf(AccessPermission.READ))
        }
        monitorShareRecipientsUseCase.stub {
            on { invoke(any()) }.thenReturn(
                flow {
                    emit(
                        listOf(
                            mock<ShareRecipient.Contact>(),
                            mock<ShareRecipient.NonContact>(),
                        )
                    )
                    awaitCancellation()
                }
            )
        }

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(FileContactListState.Data::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that share folder use case is called with correct parameters when removeShare is called`() =
        runTest {
            monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
            initUnderTest()

            underTest.removeShare(
                listOf(
                    TestValues.contactMock,
                    TestValues.nonContactMock,
                )
            )

            verify(shareFolderUseCase).invoke(
                nodeIds = listOf(TestValues.NODE_ID),
                contactData = listOf(TestValues.CONTACT_EMAIL, TestValues.NON_CONTACT_EMAIL),
                accessPermission = AccessPermission.UNKNOWN
            )
        }

    @Test
    fun `test that error removing a share returns an error message in shareRemovedEvent`() =
        runTest {
            monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
            shareFolderUseCase.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenAnswer { throw RuntimeException("Error") }
            }
            initUnderTest()

            underTest.state.test {
                assert(awaitItem() is FileContactListState.Loading)
                underTest.removeShare(
                    listOf(TestValues.contactMock)
                )
                val actual = awaitItem() as? FileContactListState.Data
                assertThat(actual?.shareRemovedEvent?.triggeredContent()).isEqualTo(TestValues.FAILURE_STRING)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that removing a share success result is displayed if returned`() = runTest {
        monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
        shareFolderUseCase.stub {
            onBlocking {
                invoke(
                    any(),
                    any(),
                    any()
                )
            }.thenReturn(
                MoveRequestResult.ShareMovement(
                    count = 1,
                    errorCount = 0,
                    nodes = listOf(TestValues.NODE_HANDLE)
                )
            )
        }
        initUnderTest()

        underTest.state.test {
            assert(awaitItem() is FileContactListState.Loading)
            underTest.removeShare(
                listOf(
                    TestValues.contactMock,
                )
            )
            val actual = awaitItem() as? FileContactListState.Data
            assertThat(actual?.shareRemovedEvent?.triggeredContent()).isEqualTo(TestValues.SUCCESS_STRING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when share removed event is handled it is set to consumed`() = runTest {
        monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
        shareFolderUseCase.stub {
            onBlocking {
                invoke(
                    any(),
                    any(),
                    any()
                )
            }.thenAnswer { throw RuntimeException("Error") }
        }
        initUnderTest()

        underTest.state.test {
            assert(awaitItem() is FileContactListState.Loading)
            underTest.removeShare(
                listOf(
                    TestValues.contactMock,
                )
            )
            assertThat((awaitItem() as? FileContactListState.Data)?.shareRemovedEvent?.triggeredContent()).isEqualTo(
                TestValues.FAILURE_STRING
            )
            underTest.onShareRemovedEventHandled()
            assertThat((awaitItem() as? FileContactListState.Data)?.shareRemovedEvent).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that share folder use case is called with correct parameters when shareFolder is called`() =
        runTest {
            monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
            initUnderTest()

            val expectedPermission = AccessPermission.READWRITE
            underTest.shareFolder(
                listOf(
                    TestValues.CONTACT_EMAIL,
                    TestValues.NON_CONTACT_EMAIL,
                ),
                expectedPermission
            )

            verify(shareFolderUseCase).invoke(
                nodeIds = listOf(TestValues.NODE_ID),
                contactData = listOf(TestValues.CONTACT_EMAIL, TestValues.NON_CONTACT_EMAIL),
                accessPermission = expectedPermission
            )
        }

    @Test
    fun `test that sharing in progress is true while the use case is in progress`() = runTest {
        monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
        shareFolderRequestMapper.stub { on { invoke(any()) }.thenReturn(TestValues.SUCCESS_STRING) }
        val gate = CompletableDeferred<MoveRequestResult.ShareMovement>()
        shareFolderUseCase.stub {
            onBlocking {
                invoke(
                    any(),
                    any(),
                    any()
                )
            }.doSuspendableAnswer {
                gate.await()
            }
        }
        val result = MoveRequestResult.ShareMovement(
            count = 1,
            errorCount = 0,
            nodes = listOf(TestValues.NODE_HANDLE)
        )
        val expectedPermission = AccessPermission.READWRITE
        initUnderTest()

        underTest.state.test {
            assert(awaitItem() is FileContactListState.Loading)
            underTest.shareFolder(
                listOf(
                    TestValues.CONTACT_EMAIL,
                ),
                expectedPermission,
            )
            val sharingStartedEvent = awaitItem() as? FileContactListState.Data
            assertThat(sharingStartedEvent?.sharingCompletedEvent).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
            assertThat(sharingStartedEvent?.sharingInProgress).isTrue()
            gate.complete(result)
            val actual = awaitItem() as? FileContactListState.Data
            assertThat(actual?.sharingCompletedEvent?.triggeredContent()).isEqualTo(TestValues.SUCCESS_STRING)
            assertThat(actual?.sharingInProgress).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that error sharing a folder returns an error message in sharingCompletedEvent`() =
        runTest {
            monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
            shareFolderRequestMapper.stub { on { invoke(any()) }.thenReturn(TestValues.FAILURE_STRING) }
            shareFolderUseCase.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenAnswer { throw RuntimeException("Error") }
            }
            val expectedPermission = AccessPermission.READWRITE
            initUnderTest()

            underTest.state.test {
                assert(awaitItem() is FileContactListState.Loading)
                underTest.shareFolder(
                    listOf(TestValues.CONTACT_EMAIL),
                    expectedPermission,
                )
                val actual = awaitItem() as? FileContactListState.Data
                assertThat(actual?.sharingCompletedEvent?.triggeredContent()).isEqualTo(TestValues.FAILURE_STRING)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a share folder success result is displayed if returned`() = runTest {
        monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
        shareFolderRequestMapper.stub { on { invoke(any()) }.thenReturn(TestValues.SUCCESS_STRING) }
        shareFolderUseCase.stub {
            onBlocking {
                invoke(
                    any(),
                    any(),
                    any()
                )
            }.thenReturn(
                MoveRequestResult.ShareMovement(
                    count = 1,
                    errorCount = 0,
                    nodes = listOf(TestValues.NODE_HANDLE)
                )
            )
        }
        val expectedPermission = AccessPermission.READWRITE
        initUnderTest()

        underTest.state.test {
            assert(awaitItem() is FileContactListState.Loading)
            underTest.shareFolder(
                listOf(
                    TestValues.CONTACT_EMAIL,
                ),
                expectedPermission,
            )
            val actual = awaitItem() as? FileContactListState.Data
            assertThat(actual?.sharingCompletedEvent?.triggeredContent()).isEqualTo(TestValues.SUCCESS_STRING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when share folder completed event is handled it is set to consumed`() = runTest {
        monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
        shareFolderRequestMapper.stub { on { invoke(any()) }.thenReturn(TestValues.FAILURE_STRING) }
        shareFolderUseCase.stub {
            onBlocking {
                invoke(
                    any(),
                    any(),
                    any()
                )
            }.thenAnswer { throw RuntimeException("Error") }
        }
        val expectedPermission = AccessPermission.READWRITE
        initUnderTest()

        underTest.state.test {
            assert(awaitItem() is FileContactListState.Loading)
            underTest.shareFolder(
                listOf(
                    TestValues.CONTACT_EMAIL,
                ),
                expectedPermission,
            )
            assertThat((awaitItem() as? FileContactListState.Data)?.sharingCompletedEvent?.triggeredContent()).isEqualTo(
                TestValues.FAILURE_STRING
            )
            underTest.onSharingCompletedEventHandled()
            assertThat((awaitItem() as? FileContactListState.Data)?.sharingCompletedEvent).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that share folder use case is called with correct parameters when changePermissions is called`() =
        runTest {
            monitorShareRecipientsUseCase.stub { on { invoke(any()) }.thenReturn(flow { awaitCancellation() }) }
            initUnderTest()

            val newPermission = AccessPermission.READWRITE
            underTest.changePermissions(
                listOf(
                    TestValues.contactMock,
                    TestValues.nonContactMock,
                ),
                newPermission
            )

            verify(shareFolderUseCase).invoke(
                nodeIds = listOf(TestValues.NODE_ID),
                contactData = listOf(TestValues.CONTACT_EMAIL, TestValues.NON_CONTACT_EMAIL),
                accessPermission = newPermission
            )
        }

    @Test
    fun `test that ui data state contains the expected data`() = runTest {
        val expectedPermissions = setOf(AccessPermission.FULL, AccessPermission.READ)
        val expectedRecipients = listOf(
            mock<ShareRecipient.Contact>(),
            mock<ShareRecipient.NonContact>(),
        )
        getAllowedSharingPermissionsUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(expectedPermissions)
        }
        monitorShareRecipientsUseCase.stub {
            on { invoke(any()) }.thenReturn(
                flow {
                    emit(
                        expectedRecipients
                    )
                    awaitCancellation()
                }
            )
        }

        initUnderTest()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual).isInstanceOf(FileContactListState.Data::class.java)
            assertThat((actual as FileContactListState.Data).recipients).isEqualTo(
                expectedRecipients
            )
            assertThat(actual.accessPermissions).isEqualTo(expectedPermissions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that show verified contact warning field is set to true if use case returns true`() =
        runTest {
            getContactVerificationWarningUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }

            initUnderTest()

            underTest.state.test {
                val actual = awaitItem()
                assertThat((actual as FileContactListState.Data).isContactVerificationWarningEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }


    private data object TestValues {
        const val CONTACT_EMAIL = "contactEmail"
        val contactMock = mock<ShareRecipient.Contact> {
            on { email }.thenReturn(CONTACT_EMAIL)
        }
        const val NON_CONTACT_EMAIL = "nonContactEmail"
        val nonContactMock = mock<ShareRecipient.NonContact> {
            on { email }.thenReturn(NON_CONTACT_EMAIL)
        }
        const val FAILURE_STRING = "NoSuccessResult"
        const val SUCCESS_STRING = "NoSuccessResult"
        const val NODE_HANDLE = 123L
        val NODE_ID = NodeId(NODE_HANDLE)
        const val FOLDER_NAME = "folderName"
    }
}