package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaExceptionMapperTest {
    private lateinit var underTest: MegaExceptionMapper

    @BeforeAll
    fun setUp() {
        underTest = MegaExceptionMapper()
    }

    @ParameterizedTest
    @MethodSource("provideErrorsAndClass")
    fun `test that error is mapped to correct class`(
        error: MegaError,
        expectedClass: Class<MegaException>,
    ) {
        Truth.assertThat(underTest(error)).isInstanceOf(expectedClass)
    }

    @ParameterizedTest
    @MethodSource("provideErrorsAndClass")
    fun `test that error is mapped with correct values`(
        error: MegaError,
    ) {
        val methodName = "methodName${error.errorCode}"
        val mapped = underTest(error, methodName)
        Truth.assertThat(mapped.errorCode).isEqualTo(error.errorCode)
        Truth.assertThat(mapped.errorString).isEqualTo(error.errorString)
        Truth.assertThat(mapped.value).isEqualTo(error.value)
        Truth.assertThat(mapped.methodName).isEqualTo(methodName)
    }

    private fun provideErrorsAndClass() = errorCodeToClass().map { (code, clazz) ->
        Arguments.of(
            mock<MegaError> {
                on { errorCode }.thenReturn(code)
                on { errorString }.thenReturn(code.toString())
                on { value }.thenReturn(code.toLong())
            },
            clazz
        )
    }

    private fun errorCodeToClass() = hashMapOf(
        MegaError.API_EOVERQUOTA to QuotaExceededMegaException::class.java,
        MegaError.API_EGOINGOVERQUOTA to NotEnoughQuotaMegaException::class.java,
        MegaError.API_EBLOCKED to BlockedMegaException::class.java,
        MegaError.API_EBUSINESSPASTDUE to BusinessAccountExpiredMegaException::class.java,
    )
}