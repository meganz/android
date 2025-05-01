package mega.privacy.android.domain.usecase.shares

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

class MonitorShareRecipientsUseCaseTest {
    lateinit var underTest: MonitorShareRecipientsUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val contactsRepository = mock<ContactsRepository>()
    private val avatarRepository = mock<AvatarRepository>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorShareRecipientsUseCase(
            nodeRepository = nodeRepository,
            contactsRepository = contactsRepository,
            avatarRepository = avatarRepository,
        )
    }

    @AfterEach
    fun cleanUp() {
        reset(nodeRepository, contactsRepository, avatarRepository)
    }

    @Test
    fun `test that a node without shares returns an empty list`() = runTest {
        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(emptyList())
        }

        underTest.invoke(NodeId(1)).test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when no contact is found, a non contact recipient is returned`() = runTest {
        val nonContactEmail = "nonContactEmail@test.com"
        val isPending = true
        val access = AccessPermission.UNKNOWN
        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(
                listOf(
                    ShareData(
                        user = nonContactEmail,
                        userFullName = null,
                        nodeHandle = 1,
                        access = access,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                )
            )
            on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
        }
        contactsRepository.stub {
            on { monitorContactByEmail(nonContactEmail) }.thenReturn(flow {
                emit(null)
                awaitCancellation()
            })
        }

        underTest(NodeId(1)).test {
            assertThat(awaitItem()).containsExactly(
                ShareRecipient.NonContact(
                    email = nonContactEmail,
                    permission = access,
                    isPending = isPending,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when a contact is found a contact recipient item is emitted`() = runTest {
        val contactEmail = "contactEmail@test.com"
        val isPending = true
        val access = AccessPermission.UNKNOWN
        val nickname = "nickname"
        val userId = 1L
        val avatarColour = 42

        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(
                listOf(
                    ShareData(
                        user = contactEmail,
                        userFullName = null,
                        nodeHandle = 1,
                        access = access,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                )
            )
            on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
        }
        contactsRepository.stub {
            on { monitorContactByEmail(contactEmail) }.thenReturn(flow {
                emit(
                    getContact(userId, contactEmail, nickname)
                )
                awaitCancellation()
            })
            onBlocking { getAvatarUri(contactEmail) }.thenReturn(null)
            on { monitorOnlineStatusByHandle(userId) }.thenReturn(flow {
                emit(UserChatStatus.Invalid)
                awaitCancellation()
            })
            onBlocking { areCredentialsVerified(contactEmail) }.thenReturn(false)
        }
        avatarRepository.stub {
            onBlocking { getAvatarColor(userId) }.thenReturn(avatarColour)
        }

        underTest(NodeId(1)).test {
            assertThat(awaitItem()).containsExactly(
                ShareRecipient.Contact(
                    handle = userId,
                    email = contactEmail,
                    contactData = ContactData(
                        fullName = nickname,
                        alias = nickname,
                        avatarUri = null,
                        userVisibility = UserVisibility.Visible
                    ),
                    isVerified = false,
                    permission = access,
                    isPending = isPending,
                    status = UserChatStatus.Invalid,
                    defaultAvatarColor = avatarColour,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when a contact is updated a new recipient item is emitted`() = runTest {
        val contactEmail = "contactEmail@test.com"
        val isPending = true
        val access = AccessPermission.UNKNOWN
        val nickname = "nickname"
        val newNickName = "newNickname"
        val userId = 1L
        val avatarColour = 42

        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(
                listOf(
                    ShareData(
                        user = contactEmail,
                        userFullName = null,
                        nodeHandle = 1,
                        access = access,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                )
            )
            on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
        }
        val contactFlow = MutableStateFlow(getContact(userId, contactEmail, nickname))
        contactsRepository.stub {
            on { monitorContactByEmail(contactEmail) }.thenReturn(contactFlow)
            onBlocking { getAvatarUri(contactEmail) }.thenReturn(null)
            on { monitorOnlineStatusByHandle(userId) }.thenReturn(flow {
                emit(UserChatStatus.Invalid)
                awaitCancellation()
            })
            onBlocking { areCredentialsVerified(contactEmail) }.thenReturn(false)
        }
        avatarRepository.stub {
            onBlocking { getAvatarColor(userId) }.thenReturn(avatarColour)
        }

        underTest(NodeId(1)).test {
            assertThat(awaitItem()).containsExactly(
                ShareRecipient.Contact(
                    handle = userId,
                    email = contactEmail,
                    contactData = ContactData(
                        fullName = nickname,
                        alias = nickname,
                        avatarUri = null,
                        userVisibility = UserVisibility.Visible
                    ),
                    isVerified = false,
                    permission = access,
                    isPending = isPending,
                    status = UserChatStatus.Invalid,
                    defaultAvatarColor = avatarColour,
                )
            )
            contactFlow.emit(getContact(userId, contactEmail, newNickName))
            assertThat(awaitItem()).containsExactly(
                ShareRecipient.Contact(
                    handle = userId,
                    email = contactEmail,
                    contactData = ContactData(
                        fullName = newNickName,
                        alias = newNickName,
                        avatarUri = null,
                        userVisibility = UserVisibility.Visible
                    ),
                    isVerified = false,
                    permission = access,
                    isPending = isPending,
                    status = UserChatStatus.Invalid,
                    defaultAvatarColor = avatarColour,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates to user online status are emitted`() = runTest {
        val contactEmail = "contactEmail@test.com"
        val isPending = true
        val access = AccessPermission.UNKNOWN
        val nickname = "nickname"
        val userId = 1L

        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(
                listOf(
                    ShareData(
                        user = contactEmail,
                        userFullName = null,
                        nodeHandle = 1,
                        access = access,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                )
            )
            on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
        }
        val firstStatus = UserChatStatus.Away
        val secondStatus = UserChatStatus.Online
        val statusFlow = MutableStateFlow(firstStatus)

        contactsRepository.stub {
            on { monitorContactByEmail(contactEmail) }.thenReturn(flow {
                emit(
                    getContact(userId, contactEmail, nickname)
                )
                awaitCancellation()
            })
            onBlocking { getAvatarUri(contactEmail) }.thenReturn(null)
            on { monitorOnlineStatusByHandle(userId) }.thenReturn(statusFlow)
            onBlocking { areCredentialsVerified(contactEmail) }.thenReturn(false)
        }

        avatarRepository.stub {
            onBlocking { getAvatarColor(userId) }.thenReturn(42)
        }

        underTest(NodeId(1)).map { it.filterIsInstance<ShareRecipient.Contact>().first().status }
            .test {
                assertThat(awaitItem()).isEqualTo(firstStatus)
                statusFlow.emit(secondStatus)
                assertThat(awaitItem()).isEqualTo(secondStatus)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that multiple contacts and non contacts are all returned`() = runTest {
        val contactEmail = "@test.com"
        val nonContactEmail = "NC@test.com"
        val isPending = true
        val access = AccessPermission.UNKNOWN
        val nickname = "nickname"
        val avatarColour = 42

        val range = 1..9

        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(
                range.map {
                    ShareData(
                        user = if (it.rem(2) == 0) "$it$contactEmail" else "$it$nonContactEmail",
                        userFullName = null,
                        nodeHandle = 1,
                        access = access,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                }
            )
            on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
        }
        contactsRepository.stub {
            on { monitorContactByEmail(any()) }.thenAnswer { invocation ->
                val email = invocation.getArgument<String>(0)
                val userId = email[0].toString().toLong()
                if (userId.rem(2) == 0L) {
                    flow {
                        emit(
                            getContact(userId, email, "$userId$nickname")
                        )
                        awaitCancellation()
                    }
                } else {
                    flow {
                        emit(null)
                        awaitCancellation()
                    }
                }
            }
            onBlocking { getAvatarUri(contactEmail) }.thenReturn(null)
            on { monitorOnlineStatusByHandle(any()) }.thenReturn(flow {
                emit(UserChatStatus.Invalid)
                awaitCancellation()
            })
            onBlocking { areCredentialsVerified(any()) }.thenReturn(false)
        }
        avatarRepository.stub {
            onBlocking { getAvatarColor(any()) }.thenReturn(avatarColour)
        }

        underTest(NodeId(1)).test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                range.mapNotNull {
                    if (it.rem(2) == 0) {
                        ShareRecipient.Contact(
                            handle = it.toLong(),
                            email = "$it$contactEmail",
                            contactData = ContactData(
                                fullName = "$it$nickname",
                                alias = "$it$nickname",
                                avatarUri = null,
                                userVisibility = UserVisibility.Visible
                            ),
                            isVerified = false,
                            permission = access,
                            isPending = isPending,
                            status = UserChatStatus.Invalid,
                            defaultAvatarColor = avatarColour,
                        )
                    } else {
                        ShareRecipient.NonContact(
                            email = "$it$nonContactEmail",
                            permission = access,
                            isPending = isPending,
                        )
                    }
                }
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that contacts are updated if permission changes`() = runTest {
        val contactEmail = "contactEmail@test.com"
        val isPending = true
        val access = AccessPermission.UNKNOWN
        val newAccess = AccessPermission.READWRITE
        val nickname = "nickname"
        val userId = 1L
        val avatarColour = 42
        val sharedNodeId = NodeId(123)

        val nodeUpdateFlow = MutableStateFlow<NodeUpdate>(NodeUpdate(emptyMap()))
        nodeRepository.stub {
            onBlocking { getNodeOutgoingShares(any()) }.thenReturn(
                listOf(
                    ShareData(
                        user = contactEmail,
                        userFullName = null,
                        nodeHandle = 1,
                        access = access,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                ),
                listOf(
                    ShareData(
                        user = contactEmail,
                        userFullName = null,
                        nodeHandle = 1,
                        access = newAccess,
                        timeStamp = 0,
                        isVerified = false,
                        isPending = isPending,
                        isContactCredentialsVerified = false,
                        count = 1
                    )
                )
            )
            on { monitorNodeUpdates() }.thenReturn(
                nodeUpdateFlow
            )
        }
        contactsRepository.stub {
            on { monitorContactByEmail(contactEmail) }.thenReturn(flow {
                emit(
                    getContact(userId, contactEmail, nickname)
                )
                awaitCancellation()
            })
            onBlocking { getAvatarUri(contactEmail) }.thenReturn(null)
            on { monitorOnlineStatusByHandle(userId) }.thenReturn(flow {
                emit(UserChatStatus.Invalid)
                awaitCancellation()
            })
            onBlocking { areCredentialsVerified(contactEmail) }.thenReturn(false)
        }
        avatarRepository.stub {
            onBlocking { getAvatarColor(userId) }.thenReturn(avatarColour)
        }

        underTest(sharedNodeId).test {
            assertThat(awaitItem()).containsExactly(
                ShareRecipient.Contact(
                    handle = userId,
                    email = contactEmail,
                    contactData = ContactData(
                        fullName = nickname,
                        alias = nickname,
                        avatarUri = null,
                        userVisibility = UserVisibility.Visible
                    ),
                    isVerified = false,
                    permission = access,
                    isPending = isPending,
                    status = UserChatStatus.Invalid,
                    defaultAvatarColor = avatarColour,
                )
            )
            val mockNode = mock<Node>{
                on { id }.thenReturn(sharedNodeId)
            }
            nodeUpdateFlow.emit(
                NodeUpdate(
                    mapOf(
                        mockNode to listOf(
                            NodeChanges.Outshare
                        )
                    )
                )
            )
            assertThat(awaitItem()).containsExactly(
                ShareRecipient.Contact(
                    handle = userId,
                    email = contactEmail,
                    contactData = ContactData(
                        fullName = nickname,
                        alias = nickname,
                        avatarUri = null,
                        userVisibility = UserVisibility.Visible
                    ),
                    isVerified = false,
                    permission = newAccess,
                    isPending = isPending,
                    status = UserChatStatus.Invalid,
                    defaultAvatarColor = avatarColour,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun getContact(
        userId: Long,
        contactEmail: String,
        nickname: String,
    ): Contact = Contact(
        userId = userId,
        email = contactEmail,
        nickname = nickname,
        firstName = null,
        lastName = null,
        hasPendingRequest = false,
        isVisible = true
    )

}