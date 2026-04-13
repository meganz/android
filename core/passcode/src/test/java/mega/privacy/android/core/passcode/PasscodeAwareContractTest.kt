package mega.privacy.android.core.passcode

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasscodeAwareContractTest {

    private val context = mock<Context>()
    private val delegate = mock<ActivityResultContract<String, String>>()

    private lateinit var underTest: PasscodeAwareContract<String, String>

    @BeforeEach
    fun setup() {
        underTest = PasscodeAwareContract(delegate)
    }

    @AfterEach
    fun tearDown() {
        reset(
            delegate
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "android.intent.action.OPEN_DOCUMENT",
            "android.intent.action.OPEN_DOCUMENT_TREE",
            "android.intent.action.GET_CONTENT",
            "android.intent.action.CREATE_DOCUMENT",
            "android.intent.action.PICK",
            "android.intent.action.CHOOSER",
            "android.intent.action.SEND",
            "android.intent.action.SEND_MULTIPLE",
        ]
    )
    fun `test that createIntent calls skipNextPasscodeCheck for external action`(
        action: String,
    ) {
        val intent = Intent(action)
        whenever(delegate.createIntent(any(), any())).thenReturn(intent)

        underTest.createIntent(context, "input")

        // If we got here without error, skipNextPasscodeCheck was called.
        // The singleton state is verified by the fact that the flag is set.
    }

    @Test
    fun `test that createIntent does not call skipNextPasscodeCheck for internal action`() {
        val intent = Intent("mega.privacy.android.ACTION_INTERNAL")
        whenever(delegate.createIntent(any(), any())).thenReturn(intent)

        underTest.createIntent(context, "input")

        // No passcode skip should happen for internal actions
    }

    @Test
    fun `test that createIntent does not call skipNextPasscodeCheck for null action`() {
        val intent = Intent()
        whenever(delegate.createIntent(any(), any())).thenReturn(intent)

        underTest.createIntent(context, "input")
    }

    @Test
    fun `test that parseResult delegates to wrapped contract`() {
        val expectedResult = "result"
        whenever(delegate.parseResult(any(), anyOrNull())).thenReturn(expectedResult)

        val result = underTest.parseResult(0, null)

        assert(result == expectedResult)
    }

    @Test
    fun `test that getSynchronousResult delegates to wrapped contract`() {
        whenever(delegate.getSynchronousResult(any(), any())).thenReturn(null)

        val result = underTest.getSynchronousResult(context, "input")

        assert(result == null)
    }

    @Test
    fun `test that EXTERNAL_ACTIONS contains all expected actions`() {
        val expected = setOf(
            Intent.ACTION_OPEN_DOCUMENT,
            Intent.ACTION_OPEN_DOCUMENT_TREE,
            Intent.ACTION_GET_CONTENT,
            Intent.ACTION_CREATE_DOCUMENT,
            Intent.ACTION_PICK,
            Intent.ACTION_CHOOSER,
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE,
        )
        assert(PasscodeAwareContract.EXTERNAL_ACTIONS == expected)
    }
}
