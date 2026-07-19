package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaRequest
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Documents how [FakeMegaApiGateway] completes listener-based request methods: every call is
 * recorded, and the listener synchronously receives onRequestStart followed by onRequestFinish
 * with a request of the matching [MegaRequest] type and [MegaError.API_OK], unless the outcome is
 * overridden via [FakeMegaApiGateway.stubRequest].
 *
 * Listeners are real [OptionalMegaRequestListenerInterface] instances from the data module, which
 * also proves the fake satisfies Kotlin's non-null `api` parameter checks.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayRequestListenerTest {

    private lateinit var underTest: FakeMegaApiGateway

    private val node = StubMegaNode(handle = 99L, name = "node.txt")

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    private class RecordingListener {
        val events = mutableListOf<String>()
        var request: MegaRequest? = null
        var error: MegaError? = null

        val listener = OptionalMegaRequestListenerInterface(
            onRequestStart = { request ->
                events += "start"
                this.request = request
            },
            onRequestFinish = { request, error ->
                events += "finish"
                this.request = request
                this.error = error
            },
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestMethodCases")
    fun `test that listener completes successfully with the expected request type when not stubbed`(
        name: String,
        expectedType: Int,
        call: (FakeMegaApiGateway, MegaRequestListenerInterface) -> Unit,
    ) {
        val recording = RecordingListener()

        call(underTest, recording.listener)

        assertThat(recording.events).containsExactly("start", "finish").inOrder()
        assertThat(recording.request?.type).isEqualTo(expectedType)
        assertThat(recording.error?.errorCode).isEqualTo(MegaError.API_OK)
        assertThat(underTest.invocations.single().methodName).isEqualTo(name)
    }

    private fun requestMethodCases(): List<Arguments> = listOf(
        requestCase("login", MegaRequest.TYPE_LOGIN) { gateway, listener ->
            gateway.login("test@mega.nz", "password", listener)
        },
        requestCase("fetchNodes", MegaRequest.TYPE_FETCH_NODES) { gateway, listener ->
            gateway.fetchNodes(listener)
        },
        requestCase("copyNode", MegaRequest.TYPE_COPY) { gateway, listener ->
            gateway.copyNode(node, StubMegaNode(handle = 1L, name = "root"), null, listener)
        },
        requestCase("moveNode", MegaRequest.TYPE_MOVE) { gateway, listener ->
            gateway.moveNode(node, StubMegaNode(handle = 1L, name = "root"), null, listener)
        },
        requestCase("deleteNode", MegaRequest.TYPE_REMOVE) { gateway, listener ->
            gateway.deleteNode(node, listener)
        },
        requestCase("createAccount", MegaRequest.TYPE_CREATE_ACCOUNT) { gateway, listener ->
            gateway.createAccount("test@mega.nz", "password", "Test", "User", listener)
        },
        requestCase("cancelAccount", MegaRequest.TYPE_GET_CANCEL_LINK) { gateway, listener ->
            gateway.cancelAccount(listener)
        },
        requestCase(
            "multiFactorAuthEnabled",
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK,
        ) { gateway, listener ->
            gateway.multiFactorAuthEnabled("test@mega.nz", listener)
        },
        requestCase("createSupportTicket", MegaRequest.TYPE_SUPPORT_TICKET) { gateway, listener ->
            gateway.createSupportTicket("something broke", listener)
        },
    )

    @Test
    fun `test that listener receives the stubbed error when stubRequest overrides the outcome`() {
        underTest.stubRequest(
            MegaApiGateway::login,
            error = StubMegaError(MegaError.API_EMFAREQUIRED),
        )
        val recording = RecordingListener()

        underTest.login("test@mega.nz", "password", recording.listener)

        assertThat(recording.error?.errorCode).isEqualTo(MegaError.API_EMFAREQUIRED)
    }

    @Test
    fun `test that listener receives the stubbed request when stubRequest provides one`() {
        val stubbedRequest = StubMegaRequest(type = MegaRequest.TYPE_LOGIN, email = "test@mega.nz")
        underTest.stubRequest(MegaApiGateway::login, request = stubbedRequest)
        val recording = RecordingListener()

        underTest.login("test@mega.nz", "password", recording.listener)

        assertThat(recording.request).isSameInstanceAs(stubbedRequest)
        assertThat(recording.request?.email).isEqualTo("test@mega.nz")
    }

    @Test
    fun `test that stubRequest matcher only overrides matching arguments when others succeed`() {
        underTest.stubRequest(
            MegaApiGateway::login,
            error = StubMegaError(MegaError.API_ENOENT),
            matcher = { it[0] == "unknown@mega.nz" },
        )
        val unknownRecording = RecordingListener()
        val knownRecording = RecordingListener()

        underTest.login("unknown@mega.nz", "password", unknownRecording.listener)
        underTest.login("test@mega.nz", "password", knownRecording.listener)

        assertThat(unknownRecording.error?.errorCode).isEqualTo(MegaError.API_ENOENT)
        assertThat(knownRecording.error?.errorCode).isEqualTo(MegaError.API_OK)
    }

    @Test
    fun `test that the call is still recorded when the listener is null`() {
        underTest.deleteNode(node, null)

        val invocation = underTest.invocationsOf(MegaApiGateway::deleteNode).single()
        assertThat(invocation.arguments).containsExactly(node, null).inOrder()
    }

    @Test
    fun `test that invocation arguments include the listener when the method is called`() {
        val recording = RecordingListener()

        underTest.login("test@mega.nz", "password", recording.listener)

        val invocation = underTest.invocationsOf(MegaApiGateway::login).single()
        assertThat(invocation.arguments)
            .containsExactly("test@mega.nz", "password", recording.listener)
            .inOrder()
    }

    @Test
    fun `test that getUserAttribute echoes the requested attribute type when not stubbed`() {
        val recording = RecordingListener()

        underTest.getUserAttribute(MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER, recording.listener)

        assertThat(recording.request?.type).isEqualTo(MegaRequest.TYPE_GET_ATTR_USER)
        assertThat(recording.request?.paramType)
            .isEqualTo(MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER)
        assertThat(recording.error?.errorCode).isEqualTo(MegaError.API_OK)
    }

    @Test
    fun `test that account details requests carry stub details when not stubbed`() {
        val recording = RecordingListener()

        underTest.getSpecificAccountDetails(
            storage = true,
            transfer = true,
            pro = true,
            listener = recording.listener,
        )

        val request = recording.request
        assertThat(request?.type).isEqualTo(MegaRequest.TYPE_ACCOUNT_DETAILS)
        assertThat(request?.numDetails).isEqualTo(0x07)
        assertThat(request?.megaAccountDetails).isNotNull()
        assertThat(request?.megaAccountDetails?.proLevel)
            .isEqualTo(MegaAccountDetails.ACCOUNT_TYPE_FREE)
    }

    private fun requestCase(
        name: String,
        expectedType: Int,
        call: (FakeMegaApiGateway, MegaRequestListenerInterface) -> Unit,
    ): Arguments = Arguments.of(name, expectedType, call)
}
