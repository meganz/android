package mega.privacy.android.data.test.stub

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StubConstructorBackedValuesTest {

    @Test
    fun `test that StubMegaError returns the constructor values when queried`() {
        val underTest = StubMegaError(errorCode = MegaError.API_ENOENT, errorString = "Not found")

        assertThat(underTest.errorCode).isEqualTo(MegaError.API_ENOENT)
        assertThat(underTest.errorString).isEqualTo("Not found")
        assertThat(underTest.toString()).isEqualTo("Not found")
    }

    @Test
    fun `test that StubMegaError returns success defaults when constructed without arguments`() {
        val underTest = StubMegaError()

        assertThat(underTest.errorCode).isEqualTo(MegaError.API_OK)
        assertThat(underTest.errorString).isEmpty()
    }

    @Test
    fun `test that StubMegaRequest returns the constructor values when queried`() {
        val node = StubMegaNode(handle = 5L)
        val underTest = StubMegaRequest(
            type = MegaRequest.TYPE_LOGIN,
            nodeHandle = 5L,
            parentHandle = 1L,
            email = "test@mega.nz",
            name = "name",
            link = "https://mega.nz/link",
            file = "/tmp/file",
            text = "text",
            paramType = 3,
            number = 9L,
            flag = true,
            access = 2,
            transferTag = 11,
            numDetails = 1,
            publicMegaNode = node,
        )

        assertThat(underTest.type).isEqualTo(MegaRequest.TYPE_LOGIN)
        assertThat(underTest.nodeHandle).isEqualTo(5L)
        assertThat(underTest.parentHandle).isEqualTo(1L)
        assertThat(underTest.email).isEqualTo("test@mega.nz")
        assertThat(underTest.name).isEqualTo("name")
        assertThat(underTest.link).isEqualTo("https://mega.nz/link")
        assertThat(underTest.file).isEqualTo("/tmp/file")
        assertThat(underTest.text).isEqualTo("text")
        assertThat(underTest.paramType).isEqualTo(3)
        assertThat(underTest.number).isEqualTo(9L)
        assertThat(underTest.flag).isTrue()
        assertThat(underTest.access).isEqualTo(2)
        assertThat(underTest.transferTag).isEqualTo(11)
        assertThat(underTest.numDetails).isEqualTo(1)
        assertThat(underTest.publicMegaNode).isSameInstanceAs(node)
    }

    @Test
    fun `test that StubMegaNode returns the constructor values when queried`() {
        val underTest = StubMegaNode(
            handle = 10L,
            name = "photo.jpg",
            parentHandle = 1L,
            isFolder = false,
            size = 1024L,
            creationTime = 100L,
            modificationTime = 200L,
            fingerprint = "fingerprint",
            originalFingerprint = "original",
            label = MegaNode.NODE_LBL_RED,
            duration = 30,
            isFavourite = true,
            isMarkedSensitive = true,
            isExported = true,
            isTakenDown = true,
            isInShare = true,
            publicLink = "https://mega.nz/file",
            base64Handle = "aGFuZGxl",
            description = "description",
            owner = 111L,
        )

        assertThat(underTest.handle).isEqualTo(10L)
        assertThat(underTest.name).isEqualTo("photo.jpg")
        assertThat(underTest.parentHandle).isEqualTo(1L)
        assertThat(underTest.size).isEqualTo(1024L)
        assertThat(underTest.creationTime).isEqualTo(100L)
        assertThat(underTest.modificationTime).isEqualTo(200L)
        assertThat(underTest.fingerprint).isEqualTo("fingerprint")
        assertThat(underTest.originalFingerprint).isEqualTo("original")
        assertThat(underTest.label).isEqualTo(MegaNode.NODE_LBL_RED)
        assertThat(underTest.duration).isEqualTo(30)
        assertThat(underTest.isFavourite).isTrue()
        assertThat(underTest.isMarkedSensitive).isTrue()
        assertThat(underTest.isExported).isTrue()
        assertThat(underTest.isTakenDown).isTrue()
        assertThat(underTest.isInShare).isTrue()
        assertThat(underTest.isShared).isTrue()
        assertThat(underTest.getPublicLink(false)).isEqualTo("https://mega.nz/file")
        assertThat(underTest.base64Handle).isEqualTo("aGFuZGxl")
        assertThat(underTest.description).isEqualTo("description")
        assertThat(underTest.owner).isEqualTo(111L)
    }

    @Test
    fun `test that StubMegaNode derives type and isFile when isFolder is set`() {
        val folder = StubMegaNode(isFolder = true)
        val file = StubMegaNode(isFolder = false)

        assertThat(folder.type).isEqualTo(MegaNode.TYPE_FOLDER)
        assertThat(folder.isFolder).isTrue()
        assertThat(folder.isFile).isFalse()
        assertThat(file.type).isEqualTo(MegaNode.TYPE_FILE)
        assertThat(file.isFolder).isFalse()
        assertThat(file.isFile).isTrue()
    }

    @Test
    fun `test that StubMegaNode answers hasChanged from the changes bitmask`() {
        val underTest = StubMegaNode(changes = MegaNode.CHANGE_TYPE_NAME.toLong())

        assertThat(underTest.hasChanged(MegaNode.CHANGE_TYPE_NAME.toLong())).isTrue()
        assertThat(underTest.hasChanged(MegaNode.CHANGE_TYPE_PARENT.toLong())).isFalse()
    }

    @Test
    fun `test that StubMegaUser returns the constructor values when queried`() {
        val underTest = StubMegaUser(
            email = "contact@mega.nz",
            handle = 222L,
            visibility = MegaUser.VISIBILITY_HIDDEN,
            timestamp = 300L,
            changes = MegaUser.CHANGE_TYPE_AVATAR.toLong(),
        )

        assertThat(underTest.email).isEqualTo("contact@mega.nz")
        assertThat(underTest.handle).isEqualTo(222L)
        assertThat(underTest.visibility).isEqualTo(MegaUser.VISIBILITY_HIDDEN)
        assertThat(underTest.timestamp).isEqualTo(300L)
        assertThat(underTest.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong())).isTrue()
        assertThat(underTest.hasChanged(MegaUser.CHANGE_TYPE_ALIAS.toLong())).isFalse()
    }

    @Test
    fun `test that StubMegaShare returns the constructor values when queried`() {
        val underTest = StubMegaShare(
            user = "contact@mega.nz",
            nodeHandle = 10L,
            access = MegaShare.ACCESS_FULL,
            timestamp = 400L,
            isPending = true,
            isVerified = true,
        )

        assertThat(underTest.user).isEqualTo("contact@mega.nz")
        assertThat(underTest.nodeHandle).isEqualTo(10L)
        assertThat(underTest.access).isEqualTo(MegaShare.ACCESS_FULL)
        assertThat(underTest.timestamp).isEqualTo(400L)
        assertThat(underTest.isPending).isTrue()
        assertThat(underTest.isVerified).isTrue()
    }

    @Test
    fun `test that StubMegaTransfer returns the constructor values when queried`() {
        val error = StubMegaError(errorCode = MegaError.API_EOVERQUOTA)
        val underTest = StubMegaTransfer(
            type = MegaTransfer.TYPE_UPLOAD,
            tag = 7,
            uniqueId = 77L,
            fileName = "video.mp4",
            path = "/local/video.mp4",
            parentPath = "/local",
            nodeHandle = 10L,
            parentHandle = 1L,
            transferredBytes = 512L,
            totalBytes = 1024L,
            speed = 100L,
            state = MegaTransfer.STATE_COMPLETED,
            priority = BigInteger.TEN,
            stage = MegaTransfer.STAGE_SCAN.toLong(),
            isFolderTransfer = true,
            isStreamingTransfer = true,
            isFinished = true,
            appData = "appData",
            notificationNumber = 5L,
            lastErrorExtended = error,
        )

        assertThat(underTest.type).isEqualTo(MegaTransfer.TYPE_UPLOAD)
        assertThat(underTest.tag).isEqualTo(7)
        assertThat(underTest.uniqueId).isEqualTo(77L)
        assertThat(underTest.fileName).isEqualTo("video.mp4")
        assertThat(underTest.path).isEqualTo("/local/video.mp4")
        assertThat(underTest.parentPath).isEqualTo("/local")
        assertThat(underTest.nodeHandle).isEqualTo(10L)
        assertThat(underTest.parentHandle).isEqualTo(1L)
        assertThat(underTest.transferredBytes).isEqualTo(512L)
        assertThat(underTest.totalBytes).isEqualTo(1024L)
        assertThat(underTest.speed).isEqualTo(100L)
        assertThat(underTest.state).isEqualTo(MegaTransfer.STATE_COMPLETED)
        assertThat(underTest.priority).isEqualTo(BigInteger.TEN)
        assertThat(underTest.stage).isEqualTo(MegaTransfer.STAGE_SCAN.toLong())
        assertThat(underTest.isFolderTransfer).isTrue()
        assertThat(underTest.isStreamingTransfer).isTrue()
        assertThat(underTest.isFinished).isTrue()
        assertThat(underTest.appData).isEqualTo("appData")
        assertThat(underTest.notificationNumber).isEqualTo(5L)
        assertThat(underTest.lastErrorExtended).isSameInstanceAs(error)
    }

    @Test
    fun `test that StubMegaTransferData returns the constructor values when queried`() {
        val underTest = StubMegaTransferData(
            numDownloads = 2,
            numUploads = 3,
            notificationNumber = 4L,
        )

        assertThat(underTest.numDownloads).isEqualTo(2)
        assertThat(underTest.numUploads).isEqualTo(3)
        assertThat(underTest.notificationNumber).isEqualTo(4L)
    }

    @Test
    fun `test that StubMegaContactRequest returns the constructor values when queried`() {
        val underTest = StubMegaContactRequest(
            handle = 9L,
            sourceEmail = "source@mega.nz",
            sourceMessage = "hi",
            targetEmail = "target@mega.nz",
            creationTime = 100L,
            modificationTime = 200L,
            status = MegaContactRequest.STATUS_ACCEPTED,
            isOutgoing = true,
            isAutoAccepted = true,
        )

        assertThat(underTest.handle).isEqualTo(9L)
        assertThat(underTest.sourceEmail).isEqualTo("source@mega.nz")
        assertThat(underTest.sourceMessage).isEqualTo("hi")
        assertThat(underTest.targetEmail).isEqualTo("target@mega.nz")
        assertThat(underTest.creationTime).isEqualTo(100L)
        assertThat(underTest.modificationTime).isEqualTo(200L)
        assertThat(underTest.status).isEqualTo(MegaContactRequest.STATUS_ACCEPTED)
        assertThat(underTest.isOutgoing).isTrue()
        assertThat(underTest.isAutoAccepted).isTrue()
    }

    @Test
    fun `test that StubMegaUserAlert returns the constructor values when queried`() {
        val underTest = StubMegaUserAlert(
            id = 12L,
            type = 2,
            userHandle = 222L,
            nodeHandle = 10L,
            email = "contact@mega.nz",
            heading = "heading",
            title = "title",
            seen = true,
            relevant = false,
            timestamp = 100L,
            number = 3L,
            schedId = 33L,
            pcrHandle = 44L,
        )

        assertThat(underTest.id).isEqualTo(12L)
        assertThat(underTest.type).isEqualTo(2)
        assertThat(underTest.userHandle).isEqualTo(222L)
        assertThat(underTest.nodeHandle).isEqualTo(10L)
        assertThat(underTest.email).isEqualTo("contact@mega.nz")
        assertThat(underTest.heading).isEqualTo("heading")
        assertThat(underTest.title).isEqualTo("title")
        assertThat(underTest.seen).isTrue()
        assertThat(underTest.relevant).isFalse()
        assertThat(underTest.getTimestamp(0L)).isEqualTo(100L)
        assertThat(underTest.getNumber(5L)).isEqualTo(3L)
        assertThat(underTest.schedId).isEqualTo(33L)
        assertThat(underTest.pcrHandle).isEqualTo(44L)
    }

    @Test
    fun `test that StubMegaSet returns the constructor values when queried`() {
        val underTest = StubMegaSet(
            id = 1L,
            publicId = 2L,
            user = 111L,
            ts = 100L,
            cts = 50L,
            name = "Album",
            cover = 10L,
            isExported = true,
            isTakenDown = true,
        )

        assertThat(underTest.id()).isEqualTo(1L)
        assertThat(underTest.publicId()).isEqualTo(2L)
        assertThat(underTest.user()).isEqualTo(111L)
        assertThat(underTest.ts()).isEqualTo(100L)
        assertThat(underTest.cts()).isEqualTo(50L)
        assertThat(underTest.name()).isEqualTo("Album")
        assertThat(underTest.cover()).isEqualTo(10L)
        assertThat(underTest.isExported).isTrue()
        assertThat(underTest.isTakenDown).isTrue()
    }

    @Test
    fun `test that StubMegaSetElement returns the constructor values when queried`() {
        val underTest = StubMegaSetElement(
            id = 1L,
            node = 10L,
            setId = 2L,
            order = 3L,
            ts = 100L,
            name = "element",
        )

        assertThat(underTest.id()).isEqualTo(1L)
        assertThat(underTest.node()).isEqualTo(10L)
        assertThat(underTest.setId()).isEqualTo(2L)
        assertThat(underTest.order()).isEqualTo(3L)
        assertThat(underTest.ts()).isEqualTo(100L)
        assertThat(underTest.name()).isEqualTo("element")
    }

    @Test
    fun `test that StubMegaRecentActionBucket returns the constructor values when queried`() {
        val nodes = StubMegaNodeList(listOf(StubMegaNode(handle = 10L)))
        val underTest = StubMegaRecentActionBucket(
            timestamp = 100L,
            userEmail = "test@mega.nz",
            parentHandle = 1L,
            isUpdate = true,
            isMedia = true,
            id = "bucket-id",
            nodes = nodes,
        )

        assertThat(underTest.timestamp).isEqualTo(100L)
        assertThat(underTest.userEmail).isEqualTo("test@mega.nz")
        assertThat(underTest.parentHandle).isEqualTo(1L)
        assertThat(underTest.isUpdate).isTrue()
        assertThat(underTest.isMedia).isTrue()
        assertThat(underTest.id).isEqualTo("bucket-id")
        assertThat(underTest.nodes).isSameInstanceAs(nodes)
    }

    @Test
    fun `test that StubMegaDateSection returns the constructor values when queried`() {
        val underTest = StubMegaDateSection(
            groupId = "2026-07",
            startDate = 1L,
            endDate = 2L,
            count = 3L,
        )

        assertThat(underTest.groupId).isEqualTo("2026-07")
        assertThat(underTest.startDate).isEqualTo(1L)
        assertThat(underTest.endDate).isEqualTo(2L)
        assertThat(underTest.count).isEqualTo(3L)
    }

    @Test
    fun `test that StubMegaFlag returns the constructor values when queried`() {
        val underTest = StubMegaFlag(type = 1L, group = 2L)

        assertThat(underTest.type).isEqualTo(1L)
        assertThat(underTest.group).isEqualTo(2L)
    }

    @Test
    fun `test that StubMegaSync returns the constructor values when queried`() {
        val underTest = StubMegaSync(
            megaHandle = 10L,
            localFolder = "/local/sync",
            name = "My sync",
            lastKnownMegaFolder = "/cloud/sync",
            backupId = 20L,
            error = 1,
            warning = 2,
            type = 3,
            runState = 4,
        )

        assertThat(underTest.megaHandle).isEqualTo(10L)
        assertThat(underTest.localFolder).isEqualTo("/local/sync")
        assertThat(underTest.name).isEqualTo("My sync")
        assertThat(underTest.lastKnownMegaFolder).isEqualTo("/cloud/sync")
        assertThat(underTest.backupId).isEqualTo(20L)
        assertThat(underTest.error).isEqualTo(1)
        assertThat(underTest.warning).isEqualTo(2)
        assertThat(underTest.type).isEqualTo(3)
        assertThat(underTest.runState).isEqualTo(4)
    }

    @Test
    fun `test that StubMegaChatError returns the constructor values when queried`() {
        val underTest = StubMegaChatError(
            errorCode = MegaChatError.ERROR_ACCESS,
            errorString = "Access denied",
        )

        assertThat(underTest.errorCode).isEqualTo(MegaChatError.ERROR_ACCESS)
        assertThat(underTest.errorString).isEqualTo("Access denied")
        assertThat(underTest.toString()).isEqualTo("Access denied")
    }

    @Test
    fun `test that StubMegaChatError returns success defaults when constructed without arguments`() {
        val underTest = StubMegaChatError()

        assertThat(underTest.errorCode).isEqualTo(MegaChatError.ERROR_OK)
        assertThat(underTest.errorString).isEmpty()
    }

    @Test
    fun `test that StubMegaChatRequest returns the constructor values when queried`() {
        val peerList = StubMegaChatPeerList()
        val underTest = StubMegaChatRequest(
            type = MegaChatRequest.TYPE_CREATE_CHATROOM,
            chatHandle = 1L,
            userHandle = 222L,
            privilege = MegaChatRoom.PRIV_MODERATOR,
            text = "text",
            link = "link",
            flag = true,
            number = 9L,
            paramType = 3,
            tag = 4,
            megaChatPeerList = peerList,
        )

        assertThat(underTest.type).isEqualTo(MegaChatRequest.TYPE_CREATE_CHATROOM)
        assertThat(underTest.chatHandle).isEqualTo(1L)
        assertThat(underTest.userHandle).isEqualTo(222L)
        assertThat(underTest.privilege).isEqualTo(MegaChatRoom.PRIV_MODERATOR)
        assertThat(underTest.text).isEqualTo("text")
        assertThat(underTest.link).isEqualTo("link")
        assertThat(underTest.flag).isTrue()
        assertThat(underTest.number).isEqualTo(9L)
        assertThat(underTest.paramType).isEqualTo(3)
        assertThat(underTest.tag).isEqualTo(4)
        assertThat(underTest.megaChatPeerList).isSameInstanceAs(peerList)
    }

    @Test
    fun `test that StubMegaChatRoom returns the constructor values when queried`() {
        val underTest = StubMegaChatRoom(
            chatId = 1L,
            title = "Group chat",
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            unreadCount = 5,
            peers = listOf(222L to MegaChatRoom.PRIV_STANDARD, 333L to MegaChatRoom.PRIV_RO),
            isGroup = true,
            isPublic = true,
            isMeeting = true,
            isArchived = true,
            retentionTime = 100L,
            creationTs = 200L,
        )

        assertThat(underTest.chatId).isEqualTo(1L)
        assertThat(underTest.title).isEqualTo("Group chat")
        assertThat(underTest.ownPrivilege).isEqualTo(MegaChatRoom.PRIV_MODERATOR)
        assertThat(underTest.unreadCount).isEqualTo(5)
        assertThat(underTest.peerCount).isEqualTo(2L)
        assertThat(underTest.getPeerHandle(0L)).isEqualTo(222L)
        assertThat(underTest.getPeerHandle(1L)).isEqualTo(333L)
        assertThat(underTest.getPeerPrivilege(1L)).isEqualTo(MegaChatRoom.PRIV_RO)
        assertThat(underTest.getPeerPrivilegeByHandle(222L)).isEqualTo(MegaChatRoom.PRIV_STANDARD)
        assertThat(underTest.isGroup).isTrue()
        assertThat(underTest.isPublic).isTrue()
        assertThat(underTest.isMeeting).isTrue()
        assertThat(underTest.isArchived).isTrue()
        assertThat(underTest.retentionTime).isEqualTo(100L)
        assertThat(underTest.creationTs).isEqualTo(200L)
    }

    @Test
    fun `test that StubMegaChatRoom returns unknown privilege when the peer is not present`() {
        val underTest = StubMegaChatRoom()

        assertThat(underTest.getPeerPrivilegeByHandle(999L)).isEqualTo(MegaChatRoom.PRIV_UNKNOWN)
        assertThat(underTest.getPeerHandle(0L)).isEqualTo(-1L)
    }

    @Test
    fun `test that StubMegaChatListItem returns the constructor values when queried`() {
        val underTest = StubMegaChatListItem(
            chatId = 1L,
            title = "Chat",
            ownPrivilege = MegaChatRoom.PRIV_RO,
            unreadCount = 2,
            lastMessage = "hello",
            lastMessageId = 3L,
            lastMessageType = MegaChatMessage.TYPE_NORMAL,
            lastMessageSender = 222L,
            lastTimestamp = 100L,
            isGroup = true,
            isMeeting = true,
            peerHandle = 222L,
        )

        assertThat(underTest.chatId).isEqualTo(1L)
        assertThat(underTest.title).isEqualTo("Chat")
        assertThat(underTest.ownPrivilege).isEqualTo(MegaChatRoom.PRIV_RO)
        assertThat(underTest.unreadCount).isEqualTo(2)
        assertThat(underTest.lastMessage).isEqualTo("hello")
        assertThat(underTest.lastMessageId).isEqualTo(3L)
        assertThat(underTest.lastMessageType).isEqualTo(MegaChatMessage.TYPE_NORMAL)
        assertThat(underTest.lastMessageSender).isEqualTo(222L)
        assertThat(underTest.lastTimestamp).isEqualTo(100L)
        assertThat(underTest.isGroup).isTrue()
        assertThat(underTest.isMeeting).isTrue()
        assertThat(underTest.peerHandle).isEqualTo(222L)
    }

    @Test
    fun `test that StubMegaChatMessage returns the constructor values when queried`() {
        val underTest = StubMegaChatMessage(
            msgId = 1L,
            tempId = 2L,
            msgIndex = 3,
            userHandle = 222L,
            type = MegaChatMessage.TYPE_NODE_ATTACHMENT,
            status = MegaChatMessage.STATUS_DELIVERED,
            timestamp = 100L,
            content = "hello",
            isEdited = true,
            isDeleted = true,
        )

        assertThat(underTest.msgId).isEqualTo(1L)
        assertThat(underTest.tempId).isEqualTo(2L)
        assertThat(underTest.msgIndex).isEqualTo(3)
        assertThat(underTest.userHandle).isEqualTo(222L)
        assertThat(underTest.type).isEqualTo(MegaChatMessage.TYPE_NODE_ATTACHMENT)
        assertThat(underTest.status).isEqualTo(MegaChatMessage.STATUS_DELIVERED)
        assertThat(underTest.timestamp).isEqualTo(100L)
        assertThat(underTest.content).isEqualTo("hello")
        assertThat(underTest.isEdited).isTrue()
        assertThat(underTest.isDeleted).isTrue()
    }

    @Test
    fun `test that StubMegaChatCall returns the constructor values when queried`() {
        val participants = StubMegaHandleList(listOf(222L, 333L))
        val underTest = StubMegaChatCall(
            callId = 1L,
            chatId = 2L,
            status = MegaChatCall.CALL_STATUS_IN_PROGRESS,
            duration = 60L,
            hasLocalAudio = true,
            hasLocalVideo = true,
            isRinging = true,
            isOnHold = true,
            caller = 222L,
            numParticipants = 2,
            peeridParticipants = participants,
        )

        assertThat(underTest.callId).isEqualTo(1L)
        assertThat(underTest.chatid).isEqualTo(2L)
        assertThat(underTest.status).isEqualTo(MegaChatCall.CALL_STATUS_IN_PROGRESS)
        assertThat(underTest.duration).isEqualTo(60L)
        assertThat(underTest.hasLocalAudio()).isTrue()
        assertThat(underTest.hasLocalVideo()).isTrue()
        assertThat(underTest.isRinging).isTrue()
        assertThat(underTest.isOnHold).isTrue()
        assertThat(underTest.caller).isEqualTo(222L)
        assertThat(underTest.numParticipants).isEqualTo(2)
        assertThat(underTest.peeridParticipants).isSameInstanceAs(participants)
    }

    @Test
    fun `test that StubMegaChatPresenceConfig returns the constructor values when queried`() {
        val underTest = StubMegaChatPresenceConfig(
            onlineStatus = 1,
            isAutoawayEnabled = true,
            autoawayTimeout = 300L,
            isPersist = true,
            isPending = true,
            isLastGreenVisible = true,
        )

        assertThat(underTest.onlineStatus).isEqualTo(1)
        assertThat(underTest.isAutoawayEnabled).isTrue()
        assertThat(underTest.autoawayTimeout).isEqualTo(300L)
        assertThat(underTest.isPersist).isTrue()
        assertThat(underTest.isPending).isTrue()
        assertThat(underTest.isLastGreenVisible).isTrue()
    }

    @Test
    fun `test that StubMegaChatScheduledMeeting returns the constructor values when queried`() {
        val flags = StubMegaChatScheduledFlags(sendEmails = true)
        val rules = StubMegaChatScheduledRules(freq = 1)
        val underTest = StubMegaChatScheduledMeeting(
            chatId = 1L,
            schedId = 2L,
            parentSchedId = 3L,
            organizerUserId = 222L,
            timezone = "Pacific/Auckland",
            startDateTime = 100L,
            endDateTime = 200L,
            title = "Standup",
            description = "Daily standup",
            attributes = "attrs",
            overrides = 300L,
            cancelled = 1,
            flags = flags,
            rules = rules,
            isNew = true,
            isDeleted = true,
        )

        assertThat(underTest.chatId()).isEqualTo(1L)
        assertThat(underTest.schedId()).isEqualTo(2L)
        assertThat(underTest.parentSchedId()).isEqualTo(3L)
        assertThat(underTest.organizerUserId()).isEqualTo(222L)
        assertThat(underTest.timezone()).isEqualTo("Pacific/Auckland")
        assertThat(underTest.startDateTime()).isEqualTo(100L)
        assertThat(underTest.endDateTime()).isEqualTo(200L)
        assertThat(underTest.title()).isEqualTo("Standup")
        assertThat(underTest.description()).isEqualTo("Daily standup")
        assertThat(underTest.attributes()).isEqualTo("attrs")
        assertThat(underTest.overrides()).isEqualTo(300L)
        assertThat(underTest.cancelled()).isEqualTo(1)
        assertThat(underTest.flags()).isSameInstanceAs(flags)
        assertThat(underTest.rules()).isSameInstanceAs(rules)
        assertThat(underTest.isNew).isTrue()
        assertThat(underTest.isDeleted).isTrue()
    }
}
