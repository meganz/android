package mega.privacy.android.data.test.stub

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaChatPeerList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StubCollectionAndMutationTest {

    @Test
    fun `test that StubMegaNodeList wraps the seeded nodes when queried`() {
        val node = StubMegaNode(handle = 10L)
        val underTest = StubMegaNodeList(listOf(node))

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)).isSameInstanceAs(node)
        assertThat(underTest.get(1)).isNull()
    }

    @Test
    fun `test that StubMegaNodeList grows when a node is added`() {
        val underTest = StubMegaNodeList()

        underTest.addNode(StubMegaNode(handle = 10L))

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)?.handle).isEqualTo(10L)
    }

    @Test
    fun `test that StubMegaUserList wraps the seeded users when queried`() {
        val user = StubMegaUser(handle = 222L)
        val underTest = StubMegaUserList(listOf(user))

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)).isSameInstanceAs(user)
        assertThat(underTest.get(1)).isNull()
    }

    @Test
    fun `test that StubMegaStringList wraps the seeded strings when queried`() {
        val underTest = StubMegaStringList(listOf("one", "two"))

        assertThat(underTest.size()).isEqualTo(2)
        assertThat(underTest.get(0)).isEqualTo("one")
        assertThat(underTest.get(1)).isEqualTo("two")
        assertThat(underTest.get(2)).isNull()
    }

    @Test
    fun `test that StubMegaStringList grows when a string is added`() {
        val underTest = StubMegaStringList()

        underTest.add("tag")

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)).isEqualTo("tag")
    }

    @Test
    fun `test that StubMegaStringMap wraps the seeded entries when queried`() {
        val underTest = StubMegaStringMap(mapOf("key" to "value"))

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get("key")).isEqualTo("value")
        assertThat(underTest.get("missing")).isNull()
        assertThat(underTest.keys.size()).isEqualTo(1)
        assertThat(underTest.keys.get(0)).isEqualTo("key")
    }

    @Test
    fun `test that StubMegaStringMap stores the entry when set is called`() {
        val underTest = StubMegaStringMap()

        underTest.set("key", "value")

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get("key")).isEqualTo("value")
    }

    @Test
    fun `test that StubMegaHandleList wraps the seeded handles when queried`() {
        val underTest = StubMegaHandleList(listOf(1L, 2L))

        assertThat(underTest.size()).isEqualTo(2L)
        assertThat(underTest.get(0L)).isEqualTo(1L)
        assertThat(underTest.get(1L)).isEqualTo(2L)
        assertThat(underTest.get(2L)).isEqualTo(-1L)
    }

    @Test
    fun `test that StubMegaHandleList grows when a handle is added`() {
        val underTest = StubMegaHandleList()

        underTest.addMegaHandle(10L)

        assertThat(underTest.size()).isEqualTo(1L)
        assertThat(underTest.get(0L)).isEqualTo(10L)
    }

    @Test
    fun `test that StubMegaSetList wraps the seeded sets when queried`() {
        val set = StubMegaSet(id = 1L)
        val underTest = StubMegaSetList(listOf(set))

        assertThat(underTest.size()).isEqualTo(1L)
        assertThat(underTest.get(0L)).isSameInstanceAs(set)
        assertThat(underTest.get(1L)).isNull()
    }

    @Test
    fun `test that StubMegaSetElementList wraps the seeded elements when queried`() {
        val element = StubMegaSetElement(id = 1L)
        val underTest = StubMegaSetElementList(listOf(element))

        assertThat(underTest.size()).isEqualTo(1L)
        assertThat(underTest.get(0L)).isSameInstanceAs(element)
        assertThat(underTest.get(1L)).isNull()
    }

    @Test
    fun `test that StubMegaRecentActionBucketList wraps the seeded buckets when queried`() {
        val bucket = StubMegaRecentActionBucket(timestamp = 100L)
        val underTest = StubMegaRecentActionBucketList(listOf(bucket))

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)).isSameInstanceAs(bucket)
        assertThat(underTest.get(1)).isNull()
    }

    @Test
    fun `test that StubMegaDateSectionList wraps the seeded sections when queried`() {
        val section = StubMegaDateSection(groupId = "2026-07")
        val underTest = StubMegaDateSectionList(listOf(section))

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)).isSameInstanceAs(section)
        assertThat(underTest.get(1)).isNull()
    }

    @Test
    fun `test that StubMegaSyncList grows when a sync is added`() {
        val sync = StubMegaSync(backupId = 20L)
        val underTest = StubMegaSyncList()

        underTest.addSync(sync)

        assertThat(underTest.size()).isEqualTo(1)
        assertThat(underTest.get(0)).isSameInstanceAs(sync)
        assertThat(underTest.get(1)).isNull()
    }

    @Test
    fun `test that StubMegaChatPeerList returns peers when seeded and added`() {
        val underTest = StubMegaChatPeerList(listOf(222L to MegaChatPeerList.PRIV_STANDARD))

        underTest.addPeer(333L, MegaChatPeerList.PRIV_MODERATOR)

        assertThat(underTest.size()).isEqualTo(2)
        assertThat(underTest.getPeerHandle(0)).isEqualTo(222L)
        assertThat(underTest.getPeerPrivilege(0)).isEqualTo(MegaChatPeerList.PRIV_STANDARD)
        assertThat(underTest.getPeerHandle(1)).isEqualTo(333L)
        assertThat(underTest.getPeerPrivilege(1)).isEqualTo(MegaChatPeerList.PRIV_MODERATOR)
    }

    @Test
    fun `test that StubMegaChatPeerList returns sentinels when the index is out of bounds`() {
        val underTest = StubMegaChatPeerList()

        assertThat(underTest.getPeerHandle(0)).isEqualTo(-1L)
        assertThat(underTest.getPeerPrivilege(0)).isEqualTo(MegaChatPeerList.PRIV_UNKNOWN)
    }

    @Test
    fun `test that StubMegaCancelToken reports cancelled when cancel is called`() {
        val underTest = StubMegaCancelToken()

        assertThat(underTest.isCancelled).isFalse()

        underTest.cancel()

        assertThat(underTest.isCancelled).isTrue()
    }

    @Test
    fun `test that StubMegaCancelToken reports cancelled when seeded as cancelled`() {
        val underTest = StubMegaCancelToken(cancelled = true)

        assertThat(underTest.isCancelled).isTrue()
    }

    @Test
    fun `test that StubMegaChatScheduledFlags updates sendEmails when the setter is called`() {
        val underTest = StubMegaChatScheduledFlags()

        assertThat(underTest.sendEmails()).isFalse()
        assertThat(underTest.isEmpty).isTrue()

        underTest.setSendEmails(true)

        assertThat(underTest.sendEmails()).isTrue()
        assertThat(underTest.isEmpty).isFalse()

        underTest.reset()

        assertThat(underTest.sendEmails()).isFalse()
    }

    @Test
    fun `test that StubMegaChatScheduledRules updates values when setters are called`() {
        val underTest = StubMegaChatScheduledRules()

        underTest.setFreq(1)
        underTest.setInterval(2)
        underTest.setUntil(100L)

        assertThat(underTest.freq()).isEqualTo(1)
        assertThat(underTest.interval()).isEqualTo(2)
        assertThat(underTest.until()).isEqualTo(100L)
    }

    @Test
    fun `test that StubMegaFileServiceReclaimOptions updates values when setters are called`() {
        val underTest = StubMegaFileServiceReclaimOptions()

        underTest.setAgeThreshold(30)
        underTest.delay = 10L
        underTest.period = 20L
        underTest.reclaimThreshold = 100L
        underTest.reclaimTarget = 50L

        assertThat(underTest.ageThreshold).isEqualTo(30)
        assertThat(underTest.delay).isEqualTo(10L)
        assertThat(underTest.period).isEqualTo(20L)
        assertThat(underTest.reclaimThreshold).isEqualTo(100L)
        assertThat(underTest.reclaimTarget).isEqualTo(50L)
    }

    @Test
    fun `test that StubMegaPushNotificationSettings enables global dnd when setGlobalDnd is called`() {
        val underTest = StubMegaPushNotificationSettings()

        assertThat(underTest.isGlobalDndEnabled).isFalse()

        underTest.setGlobalDnd(100L)

        assertThat(underTest.isGlobalDndEnabled).isTrue()
        assertThat(underTest.globalDnd).isEqualTo(100L)

        underTest.disableGlobalDnd()

        assertThat(underTest.isGlobalDndEnabled).isFalse()
        assertThat(underTest.globalDnd).isEqualTo(-1L)
    }

    @Test
    fun `test that StubMegaPushNotificationSettings stores the schedule when setGlobalSchedule is called`() {
        val underTest = StubMegaPushNotificationSettings()

        underTest.setGlobalSchedule(480, 1020, "Pacific/Auckland")

        assertThat(underTest.isGlobalScheduleEnabled).isTrue()
        assertThat(underTest.globalScheduleStart).isEqualTo(480)
        assertThat(underTest.globalScheduleEnd).isEqualTo(1020)
        assertThat(underTest.globalScheduleTimezone).isEqualTo("Pacific/Auckland")

        underTest.disableGlobalSchedule()

        assertThat(underTest.isGlobalScheduleEnabled).isFalse()
    }

    @Test
    fun `test that StubMegaPushNotificationSettings updates toggles when enable methods are called`() {
        val underTest = StubMegaPushNotificationSettings()

        underTest.enableContacts(true)
        underTest.enableShares(true)
        underTest.setGlobalChatsDnd(50L)

        assertThat(underTest.isContactsEnabled).isTrue()
        assertThat(underTest.isSharesEnabled).isTrue()
        assertThat(underTest.isGlobalChatsDndEnabled).isTrue()
        assertThat(underTest.globalChatsDnd).isEqualTo(50L)
    }
}
