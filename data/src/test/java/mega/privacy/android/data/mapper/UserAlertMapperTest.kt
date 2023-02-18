package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.CustomAlert
import mega.privacy.android.domain.entity.DeletedScheduledMeetingAlert
import mega.privacy.android.domain.entity.DeletedShareAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingPendingContactReminderAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.NewScheduledMeetingAlert
import mega.privacy.android.domain.entity.NewShareAlert
import mega.privacy.android.domain.entity.NewSharedNodesAlert
import mega.privacy.android.domain.entity.PaymentFailedAlert
import mega.privacy.android.domain.entity.PaymentReminderAlert
import mega.privacy.android.domain.entity.PaymentSucceededAlert
import mega.privacy.android.domain.entity.RemovedFromShareByOwnerAlert
import mega.privacy.android.domain.entity.RemovedSharedNodesAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.TakeDownAlert
import mega.privacy.android.domain.entity.TakeDownReinstatedAlert
import mega.privacy.android.domain.entity.UnknownAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingIgnoredAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingDeniedAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUserAlert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class UserAlertMapperTest {

    @Test
    fun `test that returned type matches expected type`() = runTest {
        mapOf(
            MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED to ContactChangeAccountDeletedAlert::class.java,
            MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU to ContactChangeBlockedYouAlert::class.java,
            MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED to ContactChangeContactEstablishedAlert::class.java,
            MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU to ContactChangeDeletedYouAlert::class.java,
            MegaUserAlert.TYPE_DELETEDSHARE to DeletedShareAlert::class.java,
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED to IncomingPendingContactCancelledAlert::class.java,
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER to IncomingPendingContactReminderAlert::class.java,
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST to IncomingPendingContactRequestAlert::class.java,
            MegaUserAlert.TYPE_NEWSHARE to NewShareAlert::class.java,
            MegaUserAlert.TYPE_NEWSHAREDNODES to NewSharedNodesAlert::class.java,
            MegaUserAlert.TYPE_PAYMENT_FAILED to PaymentFailedAlert::class.java,
            MegaUserAlert.TYPE_PAYMENT_SUCCEEDED to PaymentSucceededAlert::class.java,
            MegaUserAlert.TYPE_PAYMENTREMINDER to PaymentReminderAlert::class.java,
            MegaUserAlert.TYPE_REMOVEDSHAREDNODES to RemovedSharedNodesAlert::class.java,
            MegaUserAlert.TYPE_TAKEDOWN to TakeDownAlert::class.java,
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED to TakeDownReinstatedAlert::class.java,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED to UpdatedPendingContactIncomingAcceptedAlert::class.java,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED to UpdatedPendingContactIncomingDeniedAlert::class.java,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED to UpdatedPendingContactIncomingIgnoredAlert::class.java,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED to UpdatedPendingContactOutgoingAcceptedAlert::class.java,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED to UpdatedPendingContactOutgoingDeniedAlert::class.java,
            MegaUserAlert.TYPE_SCHEDULEDMEETING_NEW to ScheduledMeetingAlert::class.java,
            MegaUserAlert.TYPE_SCHEDULEDMEETING_DELETED to DeletedScheduledMeetingAlert::class.java,
            MegaUserAlert.TYPE_SCHEDULEDMEETING_UPDATED to ScheduledMeetingAlert::class.java,
            -1 to UnknownAlert::class.java,
        ).forEach { (id, expectedType) ->
            val megaUserAlert = createMegaUserAlert(typeId = id)
            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ ->
                        Contact(
                            userId = id.toLong(),
                            email = "test@email.com",
                            nickname = null,
                            isVisible = false,
                            hasPendingRequest = false)
                    },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null }
            assertThat(actual).isInstanceOf(expectedType)
        }
    }

    @Test
    fun `test that contact alerts attempt to return email and contact information`() = runTest {
        listOf(
            MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED,
            MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU,
            MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED,
            MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU,
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED,
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER,
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED,
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED,
        ).forEachIndexed() { index, type ->
            val megaUserAlert = createMegaUserAlert(typeId = type)
            val expectedEmail = type.toString()
            val expectedContact = Contact(
                userId = index.toLong(),
                email = expectedEmail,
                nickname = "nickName",
                isVisible = false,
                hasPendingRequest = false,
            )

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> expectedContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null }

            assertThat((actual as ContactAlert).contact.email).isEqualTo(expectedEmail)
            assertThat((actual as ContactAlert).contact).isEqualTo(expectedContact)
        }
    }

    private val testContact = Contact(
        userId = 1L,
        email = null,
        nickname = null,
        isVisible = false,
        hasPendingRequest = false,
    )

    private val testSchedMeeting = ChatScheduledMeeting(
        chatId = 1L,
        schedId = 1L,
        parentSchedId = null,
        organizerUserId = null,
        timezone = null,
        startDateTime = null,
        endDateTime = null,
        title = "",
        description = "",
        attributes = null,
        overrides = null,
        flags = null,
        rules = null,
        changes = null,
    )

    private val testSchedMeetingOccurr = ChatScheduledMeetingOccurr(
        schedId = 1L,
        parentSchedId = -1,
        cancelled = 0,
        timezone = null,
        startDateTime = null,
        endDateTime = null,
        overrides = null,
    )

    @Test
    fun `test that new share has a node id`() = runTest {
        val expectedNodeId = 123L
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_NEWSHARE, nodeId = expectedNodeId)
        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { mock { on { handle }.thenReturn(expectedNodeId) } } as NewShareAlert

        assertThat(actual.nodeId).isEqualTo(expectedNodeId)
    }

    @Test
    fun `test that node id is returned when expected and is valid`() = runTest {

        mapOf(
            MegaUserAlert.TYPE_NEWSHAREDNODES to { alert: UserAlert ->
                assertThat((alert as NewSharedNodesAlert).nodeId).isEqualTo(MegaUserAlert.TYPE_NEWSHAREDNODES.toLong())
            },
            MegaUserAlert.TYPE_NEWSHARE to { alert: UserAlert ->
                assertThat((alert as NewShareAlert).nodeId).isEqualTo(MegaUserAlert.TYPE_NEWSHARE.toLong())
            },
            MegaUserAlert.TYPE_DELETEDSHARE to { alert: UserAlert ->
                assertThat((alert as DeletedShareAlert).nodeId).isEqualTo(MegaUserAlert.TYPE_DELETEDSHARE.toLong())
            },
            MegaUserAlert.TYPE_REMOVEDSHAREDNODES to { alert: UserAlert ->
                assertThat((alert as RemovedSharedNodesAlert).nodeId).isEqualTo(MegaUserAlert.TYPE_REMOVEDSHAREDNODES.toLong())
            },
        ).forEach { (typeId, assertion) ->
            val megaUserAlert =
                createMegaUserAlert(typeId = typeId, nodeId = typeId.toLong())

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { _ -> mock { on { handle }.thenReturn(typeId.toLong()) } }

            assertion(actual)
        }
    }

    @Test
    fun `test that node id is null if invalid handle is returned`() = runTest {
        val invalidNodeId = -1L

        mapOf(
            MegaUserAlert.TYPE_NEWSHAREDNODES to { alert: UserAlert -> assertThat((alert as NewSharedNodesAlert).nodeId).isNull() },
            MegaUserAlert.TYPE_NEWSHARE to { alert: UserAlert -> assertThat((alert as NewShareAlert).nodeId).isNull() },
            MegaUserAlert.TYPE_DELETEDSHARE to { alert: UserAlert -> assertThat((alert as DeletedShareAlert).nodeId).isNull() },
            MegaUserAlert.TYPE_REMOVEDSHAREDNODES to { alert: UserAlert -> assertThat((alert as RemovedSharedNodesAlert).nodeId).isNull() },
        ).forEach { (typeId, assertion) ->
            val megaUserAlert =
                createMegaUserAlert(typeId = typeId, nodeId = invalidNodeId)

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null }

            assertion(actual)
        }
    }

    @Test
    fun `test that takedown alerts return a name and path`() = runTest {
        val expectedName = "expectedName"
        val expectedPath = "expectedPath"
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_TAKEDOWN,
                name = expectedName,
                path = expectedPath)

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as TakeDownAlert

        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.path).isEqualTo(expectedPath)
    }

    @Test
    fun `test that takedown reinstated alerts return a name and path`() = runTest {
        val expectedName = "expectedName"
        val expectedPath = "expectedPath"
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_TAKEDOWN_REINSTATED,
                name = expectedName,
                path = expectedPath)

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as TakeDownReinstatedAlert

        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.path).isEqualTo(expectedPath)
    }

    @Test
    fun `test that payment reminder alerts return a heading and title`() = runTest {
        val expectedHeading = "expectedHeading"
        val expectedTitle = "expectedTitle"
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_PAYMENTREMINDER,
                heading = expectedHeading,
                title = expectedTitle)

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as PaymentReminderAlert

        assertThat(actual.heading).isEqualTo(expectedHeading)
        assertThat(actual.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `test that payment failed alerts return a heading and title`() = runTest {
        val expectedHeading = "expectedHeading"
        val expectedTitle = "expectedTitle"
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_PAYMENT_FAILED,
                heading = expectedHeading,
                title = expectedTitle)

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as PaymentFailedAlert

        assertThat(actual.heading).isEqualTo(expectedHeading)
        assertThat(actual.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `test that payment succeeded alerts return a heading and title`() = runTest {
        val expectedHeading = "expectedHeading"
        val expectedTitle = "expectedTitle"
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_PAYMENT_SUCCEEDED,
                heading = expectedHeading,
                title = expectedTitle)

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as PaymentSucceededAlert

        assertThat(actual.heading).isEqualTo(expectedHeading)
        assertThat(actual.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `test that removed shared nodes alert returns a count`() = runTest {
        val expectedCount = 41L
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_REMOVEDSHAREDNODES,
                numberResult = listOf(Pair(0L, expectedCount)))

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as RemovedSharedNodesAlert

        assertThat(actual.itemCount).isEqualTo(expectedCount)
    }

    @Test
    fun `test that new shared nodes alert returns the number of files and folders`() = runTest {
        val expectedFolderCount = 23L
        val expectedFileCount = 42L
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_NEWSHAREDNODES,
                numberResult = listOf(Pair(0L, expectedFolderCount), Pair(1L, expectedFileCount)))

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as NewSharedNodesAlert

        assertThat(actual.folderCount).isEqualTo(expectedFolderCount)
        assertThat(actual.fileCount).isEqualTo(expectedFileCount)
    }

    @Test
    fun `test that new shared nodes alert returns child nodes`() = runTest {
        val childNodeList = (0L..5L)
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_NEWSHAREDNODES,
                handleResult = childNodeList.map { Pair(it, it) })

        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as NewSharedNodesAlert

        assertThat(actual.childNodes).containsExactlyElementsIn(childNodeList)

    }

    @Test
    fun `test that custom alerts has a header`() = runTest {
        val expectedHeading = "expectedHeading"

        listOf(
            MegaUserAlert.TYPE_TAKEDOWN,
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED,
            MegaUserAlert.TYPE_PAYMENTREMINDER,
            MegaUserAlert.TYPE_PAYMENT_SUCCEEDED,
            MegaUserAlert.TYPE_PAYMENT_FAILED,
        ).forEach {
            val actual =
                toUserAlert(megaUserAlert = createMegaUserAlert(typeId = it,
                    heading = expectedHeading),
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null } as CustomAlert
            assertThat(actual.heading).isEqualTo(expectedHeading)
        }
    }

    @Test
    fun `test that incoming share alerts have a contact`() = runTest {
        val expectedEmail = "expected@Email"
        listOf(
            MegaUserAlert.TYPE_NEWSHAREDNODES,
            MegaUserAlert.TYPE_NEWSHARE,
            MegaUserAlert.TYPE_DELETEDSHARE,
            MegaUserAlert.TYPE_REMOVEDSHAREDNODES,
        ).forEachIndexed { index, type ->
            val expectedContact = Contact(
                userId = index.toLong(),
                email = expectedEmail,
                nickname = "nickName",
                isVisible = false,
                hasPendingRequest = false,
            )
            val actual =
                toUserAlert(megaUserAlert = createMegaUserAlert(typeId = type),
                    contactProvider = { _, _ -> expectedContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null } as IncomingShareAlert
            assertThat(actual.contact).isEqualTo(expectedContact)
        }
    }

    @Test
    fun `test that removed by user alert is returned if deleted share get number on 0 returns one`() =
        runTest {
            val expectedNodeId = 123L
            val megaUserAlert =
                createMegaUserAlert(typeId = MegaUserAlert.TYPE_DELETEDSHARE,
                    nodeId = expectedNodeId,
                    numberResult = listOf(
                        Pair(0L, 1L)))
            val actual =
                toUserAlert(
                    megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null }

            assertThat(actual).isInstanceOf(RemovedFromShareByOwnerAlert::class.java)
        }

    @Test
    fun `test that deleted share alert is returned if deleted share get number on 0 returns 0`() =
        runTest {
            val megaUserAlert =
                createMegaUserAlert(typeId = MegaUserAlert.TYPE_DELETEDSHARE,
                    numberResult = listOf(
                        Pair(0L, 0L)))
            val actual =
                toUserAlert(
                    megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null }

            assertThat(actual).isInstanceOf(DeletedShareAlert::class.java)
        }

    @Test
    fun `test that deleted share has null node name if node not found`() = runTest {
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_DELETEDSHARE,
                numberResult = listOf(
                    Pair(0L, 0L)))
        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { null } as DeletedShareAlert

        assertThat(actual.nodeName).isNull()
    }

    @Test
    fun `test that deleted share has node name if node found`() = runTest {
        val megaUserAlert =
            createMegaUserAlert(typeId = MegaUserAlert.TYPE_DELETEDSHARE,
                nodeId = 12L,
                numberResult = listOf(
                    Pair(0L, 0L)))
        val expectedNodeName = "ExpectedName"
        val node = mock<MegaNode> { on { name }.thenReturn(expectedNodeName) }
        val actual =
            toUserAlert(megaUserAlert = megaUserAlert,
                contactProvider = { _, _ -> testContact },
                scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
            ) { _ -> node } as DeletedShareAlert

        assertThat(actual.nodeName).isEqualTo(expectedNodeName)
    }

    @Test
    fun `test that takedown rootNodeId is null if nodeId is invalid`() = runTest {
        val invalidNodeId = -1L
        val node = mock<MegaNode> { on { handle }.thenReturn(2L) }
        mapOf(
            MegaUserAlert.TYPE_TAKEDOWN to { alert: UserAlert -> assertThat((alert as TakeDownAlert).rootNodeId).isNull() },
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED to { alert: UserAlert -> assertThat((alert as TakeDownReinstatedAlert).rootNodeId).isNull() },
        ).forEach { (typeId, assertion) ->
            val megaUserAlert =
                createMegaUserAlert(typeId = typeId, nodeId = invalidNodeId)

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { _ -> node }

            assertion(actual)
        }
    }

    @Test
    fun `test that takedown rootNodeId is null if node is null`() = runTest {
        mapOf(
            MegaUserAlert.TYPE_TAKEDOWN to { alert: UserAlert -> assertThat((alert as TakeDownAlert).rootNodeId).isNull() },
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED to { alert: UserAlert -> assertThat((alert as TakeDownReinstatedAlert).rootNodeId).isNull() },
        ).forEach { (typeId, assertion) ->
            val megaUserAlert =
                createMegaUserAlert(typeId = typeId, nodeId = 2L)

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { null }

            assertion(actual)
        }
    }

    @Test
    fun `test that rootNodeId is same as node id if it is a folder`() = runTest {
        val expectedNodeId = 2L
        val node = mock<MegaNode> {
            on { handle }.thenReturn(expectedNodeId)
            on { isFile }.thenReturn(false)
            on { isFolder }.thenReturn(true)
        }
        mapOf(
            MegaUserAlert.TYPE_TAKEDOWN to { alert: UserAlert ->
                assertThat((alert as TakeDownAlert).rootNodeId).isEqualTo(expectedNodeId)
            },
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED to { alert: UserAlert ->
                assertThat((alert as TakeDownReinstatedAlert).rootNodeId).isEqualTo(expectedNodeId)
            },
        ).forEach { (typeId, assertion) ->
            val megaUserAlert =
                createMegaUserAlert(typeId = typeId, nodeId = expectedNodeId)

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { _ -> node }

            assertion(actual)
        }
    }

    @Test
    fun `test that rootNodeId is same as parent node id if it is a file`() = runTest {
        val expectedNodeId = 2L
        val childNodeId = 4L
        val node = mock<MegaNode> {
            on { handle }.thenReturn(childNodeId)
            on { parentHandle }.thenReturn(expectedNodeId)
            on { isFile }.thenReturn(true)
            on { isFolder }.thenReturn(false)
        }
        mapOf(
            MegaUserAlert.TYPE_TAKEDOWN to { alert: UserAlert ->
                assertThat((alert as TakeDownAlert).rootNodeId).isEqualTo(expectedNodeId)
            },
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED to { alert: UserAlert ->
                assertThat((alert as TakeDownReinstatedAlert).rootNodeId).isEqualTo(expectedNodeId)
            },
        ).forEach { (typeId, assertion) ->
            val megaUserAlert =
                createMegaUserAlert(typeId = typeId, nodeId = childNodeId)

            val actual =
                toUserAlert(megaUserAlert = megaUserAlert,
                    contactProvider = { _, _ -> testContact },
                    scheduledMeetingProvider = { _, _ -> testSchedMeeting },
                    scheduledMeetingOccurrProvider = { listOf(testSchedMeetingOccurr) }
                ) { _ -> node }

            assertion(actual)
        }
    }
}

private fun createMegaUserAlert(
    typeId: Int,
    nodeId: Long? = null,
    name: String? = null,
    path: String? = null,
    heading: String? = null,
    title: String? = null,
    numberResult: List<Pair<Long, Long>>? = null,
    handleResult: List<Pair<Long, Long>>? = null,
): MegaUserAlert {
    val createdTimeIndex = 0L
    val invalidHandle: Long = -1
    return mock<MegaUserAlert> {
        on { type }.thenReturn(typeId)
        on { id }.thenReturn(1L)
        on { seen }.thenReturn(false)
        on { userHandle }.thenReturn(2L)
        on { getTimestamp(createdTimeIndex) }.thenReturn(3L)
        on { isOwnChange }.thenReturn(false)
        on { nodeHandle }.thenReturn(nodeId ?: invalidHandle)
        on { email }.thenReturn("")
        on { getHandle(any()) }.thenReturn(invalidHandle)
        on { this.name }.thenReturn(name)
        on { this.path }.thenReturn(path)
        on { this.heading }.thenReturn(heading)
        on { this.title }.thenReturn(title)
    }.apply {
        numberResult?.forEach { whenever(this.getNumber(it.first)).thenReturn(it.second) }
        handleResult?.forEach { whenever(this.getHandle(it.first)).thenReturn(it.second) }
    }
}