package mega.privacy.android.data.test.stub

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.InvocationTargetException

/**
 * Invokes every overridden method of every stub with default arguments, proving that no
 * call reaches native SDK code (which would crash the JVM given the null native pointer).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StubSdkObjectsSmokeTest {

    private fun stubs(): List<Named<Any>> = listOf<Any>(
        StubMegaError(),
        StubMegaRequest(),
        StubMegaNode(),
        StubMegaNodeList(),
        StubMegaUser(),
        StubMegaUserList(),
        StubMegaShare(),
        StubMegaStringList(),
        StubMegaStringMap(),
        StubMegaHandleList(),
        StubMegaTransfer(),
        StubMegaTransferData(),
        StubMegaContactRequest(),
        StubMegaUserAlert(),
        StubMegaSet(),
        StubMegaSetList(),
        StubMegaSetElement(),
        StubMegaSetElementList(),
        StubMegaRecentActionBucket(),
        StubMegaRecentActionBucketList(),
        StubMegaCancelToken(),
        StubMegaDateSection(),
        StubMegaDateSectionList(),
        StubMegaFileServiceReclaimOptions(),
        StubMegaFlag(),
        StubMegaSync(),
        StubMegaSyncList(),
        StubMegaPushNotificationSettings(),
        StubMegaChatError(),
        StubMegaChatRequest(),
        StubMegaChatRoom(),
        StubMegaChatListItem(),
        StubMegaChatMessage(),
        StubMegaChatCall(),
        StubMegaChatPeerList(),
        StubMegaChatPresenceConfig(),
        StubMegaChatScheduledMeeting(),
        StubMegaChatScheduledFlags(),
        StubMegaChatScheduledRules(),
    ).map { Named.of(it.javaClass.simpleName, it) }

    @ParameterizedTest
    @MethodSource("stubs")
    fun `test that stub does not reach native code when every overridden method is invoked`(
        stub: Any,
    ) {
        val methods = stub.javaClass.declaredMethods.filterNot { it.isSynthetic }

        methods.forEach { method ->
            method.isAccessible = true
            val arguments = method.parameterTypes.map { defaultArgumentFor(it) }.toTypedArray()
            try {
                method.invoke(stub, *arguments)
            } catch (error: InvocationTargetException) {
                throw AssertionError(
                    "${stub.javaClass.simpleName}.${method.name} threw ${error.targetException}",
                    error.targetException,
                )
            }
        }

        assertThat(methods).isNotEmpty()
    }

    private fun defaultArgumentFor(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Double.TYPE -> 0.0
        java.lang.Float.TYPE -> 0f
        String::class.java -> ""
        else -> null
    }
}
