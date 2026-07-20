package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaNodeList
import mega.privacy.android.data.test.stub.StubMegaRecentActionBucket
import mega.privacy.android.data.test.stub.StubMegaRecentActionBucketList
import mega.privacy.android.data.test.stub.StubMegaUser
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaShare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Documents the "empty success" defaults of [FakeMegaApiGateway] for unstubbed query methods:
 * null for nullable reads, false/true for capability flags, zero for counters, empty collections
 * for list reads, SDK sentinel constants where the real API defines them, and argument
 * pass-through for identity-style helpers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayDefaultsTest {

    private lateinit var underTest: FakeMegaApiGateway

    private val node = StubMegaNode(handle = 99L, name = "node.txt")

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullDefaultCases")
    fun `test that nullable query returns null when not stubbed`(
        name: String,
        call: suspend (FakeMegaApiGateway) -> Any?,
    ) = runTest {
        assertThat(call(underTest)).isNull()
    }

    private fun nullDefaultCases(): List<Arguments> = listOf(
        caseOf("getNodeByPath") { it.getNodeByPath("/missing", null) },
        caseOf("getFingerprint") { it.getFingerprint("/no/such/file") },
        caseOf("getNodeByFingerprint") { it.getNodeByFingerprint("fingerprint") },
        caseOf("getTransferByTag") { it.getTransferByTag(1) },
        caseOf("getTransferByUniqueId") { it.getTransferByUniqueId(1L) },
        caseOf("getContact") { it.getContact("someone@mega.nz") },
        caseOf("getContactRequestByHandle") { it.getContactRequestByHandle(1L) },
        caseOf("httpServerGetLocalLink") { it.httpServerGetLocalLink(node) },
        caseOf("getUserFromInShare") { it.getUserFromInShare(node, false) },
        caseOf("getSet") { it.getSet(1L) },
        caseOf("getVerifiedPhoneNumber") { it.getVerifiedPhoneNumber() },
        caseOf("getTransferData") { it.getTransferData() },
        caseOf("unSerializeNode") { it.unSerializeNode("serialized") },
        caseOf("getAllNodeTags") { it.getAllNodeTags("query", null) },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("falseDefaultCases")
    fun `test that boolean query returns false when not stubbed`(
        name: String,
        call: suspend (FakeMegaApiGateway) -> Any?,
    ) = runTest {
        assertThat(call(underTest)).isEqualTo(false)
    }

    private fun falseDefaultCases(): List<Arguments> = listOf(
        caseOf("isMasterBusinessAccount") { it.isMasterBusinessAccount() },
        caseOf("areUploadTransfersPaused") { it.areUploadTransfersPaused() },
        caseOf("areDownloadTransfersPaused") { it.areDownloadTransfersPaused() },
        caseOf("hasVersion") { it.hasVersion(node) },
        caseOf("isPendingShare") { it.isPendingShare(node) },
        caseOf("isSensitiveInherited") { it.isSensitiveInherited(node) },
        caseOf("areCredentialsVerified") { it.areCredentialsVerified(StubMegaUser()) },
        caseOf("isCurrentPassword") { it.isCurrentPassword("password") },
        caseOf("isAccountNew") { it.isAccountNew() },
        caseOf("isForeignNode") { it.isForeignNode(1L) },
        caseOf("createThumbnail") { it.createThumbnail("/img.jpg", "/thumb.jpg") },
        caseOf("createPreview") { it.createPreview("/img.jpg", "/preview.jpg") },
        caseOf("isCookieBannerEnabled") { it.isCookieBannerEnabled() },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("trueDefaultCases")
    fun `test that capability flag returns true when not stubbed`(
        name: String,
        call: suspend (FakeMegaApiGateway) -> Any?,
    ) = runTest {
        assertThat(call(underTest)).isEqualTo(true)
    }

    private fun trueDefaultCases(): List<Arguments> = listOf(
        caseOf("multiFactorAuthAvailable") { it.multiFactorAuthAvailable() },
        caseOf("isBusinessAccountActive") { it.isBusinessAccountActive() },
        caseOf("httpServerStart") { it.httpServerStart() },
        caseOf("isChatNotifiable") { it.isChatNotifiable(1L) },
        caseOf("serverSideRubbishBinAutopurgeEnabled") {
            it.serverSideRubbishBinAutopurgeEnabled()
        },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("zeroDefaultCases")
    fun `test that numeric query returns zero when not stubbed`(
        name: String,
        call: suspend (FakeMegaApiGateway) -> Any?,
    ) = runTest {
        assertThat((call(underTest) as Number).toLong()).isEqualTo(0L)
    }

    private fun zeroDefaultCases(): List<Arguments> = listOf(
        caseOf("getNumVersions") { it.getNumVersions(node) },
        caseOf("getNumUnreadUserAlerts") { it.getNumUnreadUserAlerts() },
        caseOf("httpServerIsRunning") { it.httpServerIsRunning() },
        caseOf("getPasswordStrength") { it.getPasswordStrength("password") },
        caseOf("getSmsAllowedState") { it.getSmsAllowedState() },
        caseOf("getABTestValue") { it.getABTestValue("flag") },
        caseOf("getBandwidthOverQuotaDelay") { it.getBandwidthOverQuotaDelay() },
        caseOf("getNumNodes") { it.getNumNodes() },
        caseOf("getOverquotaDeadlineTs") { it.getOverquotaDeadlineTs() },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyListDefaultCases")
    fun `test that list query returns an empty list when not stubbed`(
        name: String,
        call: suspend (FakeMegaApiGateway) -> Any?,
    ) = runTest {
        assertThat(call(underTest) as List<*>).isEmpty()
    }

    private fun emptyListDefaultCases(): List<Arguments> = listOf(
        caseOf("getVersions") { it.getVersions(node) },
        caseOf("getIncomingSharesNode") { it.getIncomingSharesNode(null) },
        caseOf("getOutgoingSharesNode") { it.getOutgoingSharesNode(null) },
        caseOf("getTransfers") { it.getTransfers(0) },
        caseOf("getUserAlerts") { it.getUserAlerts() },
        caseOf("getIncomingContactRequests") { it.getIncomingContactRequests() },
        caseOf("getOutgoingContactRequests") { it.getOutgoingContactRequests() },
        caseOf("getContacts") { it.getContacts() },
        caseOf("getUnverifiedIncomingShares") { it.getUnverifiedIncomingShares(0) },
        caseOf("getVerifiedIncomingShares") { it.getVerifiedIncomingShares(null) },
        caseOf("getOverquotaWarningsTs") { it.getOverquotaWarningsTs() },
        caseOf("getNodesByFingerprint") { it.getNodesByFingerprint("fingerprint") },
    )

    @Test
    fun `test that sentinel constants match the SDK values when not stubbed`() {
        assertThat(underTest.getWaitingReason()).isEqualTo(MegaApiJava.RETRY_NONE)
        assertThat(underTest.getInvalidHandle()).isEqualTo(MegaApiJava.INVALID_HANDLE)
        assertThat(underTest.getInvalidAffiliateType())
            .isEqualTo(MegaApiJava.AFFILIATE_TYPE_INVALID)
        assertThat(underTest.getInvalidBackupType()).isEqualTo(MegaApiJava.BACKUP_TYPE_INVALID)
    }

    @Test
    fun `test that fixed string defaults identify the fake when not stubbed`() = runTest {
        assertThat(underTest.getSdkVersion()).isEqualTo("fake-sdk")
        assertThat(underTest.generateViewId()).isEqualTo("0123456789abcdef")
        assertThat(underTest.getDeviceId()).isEqualTo("fake-device-id")
        assertThat(underTest.getExportMasterKey()).isEqualTo("fake-master-key")
        assertThat(underTest.getUserAvatarSecondaryColor(1L)).isEqualTo("#00BFA5")
    }

    @Test
    fun `test that getAccess grants owner access when not stubbed`() {
        assertThat(underTest.getAccess(node)).isEqualTo(MegaShare.ACCESS_OWNER)
    }

    @Test
    fun `test that access checks succeed when not stubbed`() {
        assertThat(underTest.checkAccessErrorExtended(node, MegaShare.ACCESS_FULL).errorCode)
            .isEqualTo(MegaError.API_OK)
        assertThat(underTest.checkMoveErrorExtended(node, node).errorCode)
            .isEqualTo(MegaError.API_OK)
    }

    @Test
    fun `test that non null container reads return empty stubs when not stubbed`() = runTest {
        assertThat(underTest.getSets().size()).isEqualTo(0L)
        assertThat(underTest.getSetElements(1L).size()).isEqualTo(0L)
        assertThat(underTest.getSyncs().size()).isEqualTo(0)
        assertThat(underTest.createCancelToken().isCancelled).isFalse()
    }

    @Test
    fun `test that identity helpers return their argument when not stubbed`() = runTest {
        val bucket = StubMegaRecentActionBucket()
        val bucketList = StubMegaRecentActionBucketList()

        assertThat(underTest.authorizeNode(node)).isSameInstanceAs(node)
        assertThat(underTest.copyBucket(bucket)).isSameInstanceAs(bucket)
        assertThat(underTest.copyBucketList(bucketList)).isSameInstanceAs(bucketList)
        assertThat(underTest.escapeFsIncompatible("file:name", "/dst")).isEqualTo("file:name")
    }

    @Test
    fun `test that getNodesFromMegaNodeList unpacks the passed list when not stubbed`() {
        val first = StubMegaNode(handle = 1L, name = "a")
        val second = StubMegaNode(handle = 2L, name = "b")

        val result = underTest.getNodesFromMegaNodeList(StubMegaNodeList(listOf(first, second)))

        assertThat(result).containsExactly(first, second).inOrder()
    }

    private fun caseOf(
        name: String,
        call: suspend (FakeMegaApiGateway) -> Any?,
    ): Arguments = Arguments.of(name, call)
}
