package mega.privacy.android.data.test.stub

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StubDefaultsTest {

    @Test
    fun `test that StubMegaNode returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaNode()

        assertThat(underTest.handle).isEqualTo(-1L)
        assertThat(underTest.name).isEmpty()
        assertThat(underTest.parentHandle).isEqualTo(-1L)
        assertThat(underTest.type).isEqualTo(MegaNode.TYPE_FILE)
        assertThat(underTest.size).isEqualTo(0L)
        assertThat(underTest.fingerprint).isNull()
        assertThat(underTest.isFavourite).isFalse()
        assertThat(underTest.isExported).isFalse()
        assertThat(underTest.isTakenDown).isFalse()
        assertThat(underTest.hasChanged(MegaNode.CHANGE_TYPE_NAME.toLong())).isFalse()
        assertThat(underTest.children).isNull()
        assertThat(underTest.serialize()).isNull()
    }

    @Test
    fun `test that StubMegaNode derives a non null base64 handle when none is provided`() {
        val underTest = StubMegaNode(handle = 12345L)

        assertThat(underTest.base64Handle)
            .isEqualTo(
                java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("12345".toByteArray())
            )
    }

    @Test
    fun `test that StubMegaRequest returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaRequest()

        assertThat(underTest.type).isEqualTo(0)
        assertThat(underTest.nodeHandle).isEqualTo(-1L)
        assertThat(underTest.email).isNull()
        assertThat(underTest.flag).isFalse()
        assertThat(underTest.publicMegaNode).isNull()
        assertThat(underTest.megaStringMap).isNull()
        assertThat(underTest.requestString).isEmpty()
    }

    @Test
    fun `test that StubMegaUser returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaUser()

        assertThat(underTest.email).isEmpty()
        assertThat(underTest.handle).isEqualTo(-1L)
        assertThat(underTest.visibility).isEqualTo(MegaUser.VISIBILITY_VISIBLE)
        assertThat(underTest.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong())).isFalse()
    }

    @Test
    fun `test that StubMegaTransfer returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaTransfer()

        assertThat(underTest.type).isEqualTo(MegaTransfer.TYPE_DOWNLOAD)
        assertThat(underTest.state).isEqualTo(MegaTransfer.STATE_NONE)
        assertThat(underTest.priority).isEqualTo(BigInteger.ZERO)
        assertThat(underTest.isFinished).isFalse()
        assertThat(underTest.lastErrorExtended).isNull()
        assertThat(underTest.appData).isNull()
    }

    @Test
    fun `test that StubMegaUserAlert returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaUserAlert()

        assertThat(underTest.seen).isFalse()
        assertThat(underTest.relevant).isTrue()
        assertThat(underTest.userHandle).isEqualTo(-1L)
        assertThat(underTest.email).isNull()
    }

    @Test
    fun `test that empty stub lists report zero size when constructed without arguments`() {
        assertThat(StubMegaNodeList().size()).isEqualTo(0)
        assertThat(StubMegaUserList().size()).isEqualTo(0)
        assertThat(StubMegaStringList().size()).isEqualTo(0)
        assertThat(StubMegaStringMap().size()).isEqualTo(0)
        assertThat(StubMegaHandleList().size()).isEqualTo(0L)
        assertThat(StubMegaSetList().size()).isEqualTo(0L)
        assertThat(StubMegaSetElementList().size()).isEqualTo(0L)
        assertThat(StubMegaRecentActionBucketList().size()).isEqualTo(0)
        assertThat(StubMegaDateSectionList().size()).isEqualTo(0)
        assertThat(StubMegaSyncList().size()).isEqualTo(0)
        assertThat(StubMegaChatPeerList().size()).isEqualTo(0)
    }

    @Test
    fun `test that StubMegaChatRoom returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaChatRoom()

        assertThat(underTest.chatId).isEqualTo(-1L)
        assertThat(underTest.title).isEmpty()
        assertThat(underTest.ownPrivilege).isEqualTo(MegaChatRoom.PRIV_STANDARD)
        assertThat(underTest.isGroup).isFalse()
        assertThat(underTest.isActive).isTrue()
        assertThat(underTest.isArchived).isFalse()
        assertThat(underTest.peerCount).isEqualTo(0L)
    }

    @Test
    fun `test that StubMegaChatCall returns benign defaults when constructed without arguments`() {
        val underTest = StubMegaChatCall()

        assertThat(underTest.callId).isEqualTo(-1L)
        assertThat(underTest.status).isEqualTo(MegaChatCall.CALL_STATUS_INITIAL)
        assertThat(underTest.hasLocalAudio()).isFalse()
        assertThat(underTest.isRinging).isFalse()
        assertThat(underTest.peeridParticipants.size()).isEqualTo(0L)
        assertThat(underTest.getMegaChatSession(0L)).isNull()
    }
}
