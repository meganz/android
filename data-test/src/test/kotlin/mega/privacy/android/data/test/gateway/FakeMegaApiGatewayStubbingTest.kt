package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents the WireMock-style stubbing and verification surface of [FakeMegaApiGateway].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayStubbingTest {

    private lateinit var underTest: FakeMegaApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    @Test
    fun `test that stubResult overrides the default when a suspend method is stubbed`() = runTest {
        underTest.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 3)

        assertThat(underTest.getNumUnreadUserAlerts()).isEqualTo(3)
    }

    @Test
    fun `test that stubResult overrides the default when a non suspend method is stubbed`() {
        underTest.stubResult(MegaApiGateway::getSdkVersion, "custom-sdk")

        assertThat(underTest.getSdkVersion()).isEqualTo("custom-sdk")
    }

    @Test
    fun `test that stubResult overrides a state backed default when the method is stubbed`() =
        runTest {
            underTest.stubResult(MegaApiGateway::getRootNode, null)

            assertThat(underTest.getRootNode()).isNull()
        }

    @Test
    fun `test that stub answer receives the call arguments when method is stubbed`() = runTest {
        underTest.stub(MegaApiGateway::getFingerprint) { arguments ->
            "fingerprint-of-${arguments[0]}"
        }

        assertThat(underTest.getFingerprint("photo.jpg")).isEqualTo("fingerprint-of-photo.jpg")
    }

    @Test
    fun `test that matcher stub only answers matching arguments when others use the default`() =
        runTest {
            val stubbedNode = StubMegaNode(handle = 7L, name = "stubbed")
            underTest.stub(
                MegaApiGateway::getMegaNodeByHandle,
                matcher = { it[0] == 7L },
            ) { stubbedNode }

            assertThat(underTest.getMegaNodeByHandle(7L)).isSameInstanceAs(stubbedNode)
            assertThat(underTest.getMegaNodeByHandle(999L)).isNull()
        }

    @Test
    fun `test that the later stub wins when the same method is stubbed twice`() = runTest {
        underTest.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 1)
        underTest.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 2)

        assertThat(underTest.getNumUnreadUserAlerts()).isEqualTo(2)
    }

    @Test
    fun `test that suspend method throws when stubError is applied`() = runTest {
        val error = IllegalStateException("boom")
        underTest.stubError(MegaApiGateway::getRootNode, error)

        val result = runCatching { underTest.getRootNode() }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that non suspend method throws when stubError is applied`() {
        val error = IllegalStateException("boom")
        underTest.stubError(MegaApiGateway::getSdkVersion, error)

        val result = runCatching { underTest.getSdkVersion() }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that invocations are recorded in call order with their arguments`() = runTest {
        underTest.getMegaNodeByHandle(1L)
        underTest.getFingerprint("file.txt")
        underTest.handleToBase64(2L)

        assertThat(underTest.invocations.map { it.methodName })
            .containsExactly("getMegaNodeByHandle", "getFingerprint", "handleToBase64")
            .inOrder()
        assertThat(underTest.invocations[0].arguments).containsExactly(1L)
        assertThat(underTest.invocations[1].arguments).containsExactly("file.txt")
        assertThat(underTest.invocations[2].arguments).containsExactly(2L)
    }

    @Test
    fun `test that invocationsOf filters recorded invocations when multiple methods were called`() =
        runTest {
            underTest.getMegaNodeByHandle(1L)
            underTest.getMegaNodeByHandle(2L)
            underTest.getFingerprint("file.txt")

            val invocations = underTest.invocationsOf(MegaApiGateway::getMegaNodeByHandle)

            assertThat(invocations).hasSize(2)
            assertThat(invocations.map { it.arguments.single() }).containsExactly(1L, 2L).inOrder()
        }

    @Test
    fun `test that stubs stay active when clearInvocations is called`() = runTest {
        underTest.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 5)
        underTest.getNumUnreadUserAlerts()

        underTest.clearInvocations()

        assertThat(underTest.invocations).isEmpty()
        assertThat(underTest.getNumUnreadUserAlerts()).isEqualTo(5)
    }

    @Test
    fun `test that defaults return when clearStubs is called`() = runTest {
        underTest.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 5)

        underTest.clearStubs()

        assertThat(underTest.getNumUnreadUserAlerts()).isEqualTo(0)
    }

    @Test
    fun `test that stubs and invocations are cleared when resetToDefaults is called`() = runTest {
        underTest.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 5)
        underTest.getNumUnreadUserAlerts()

        underTest.resetToDefaults()

        assertThat(underTest.invocations).isEmpty()
        assertThat(underTest.getNumUnreadUserAlerts()).isEqualTo(0)
    }
}
