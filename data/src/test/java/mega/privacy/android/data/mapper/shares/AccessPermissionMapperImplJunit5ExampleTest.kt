package mega.privacy.android.data.mapper.shares

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


class AccessPermissionMapperImplJunit5ExampleTest {
    private val underTest: AccessPermissionMapper = AccessPermissionMapperImpl()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(AccessPermission::class)
    fun `test mapping is not null`(expected: AccessPermission) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: AccessPermission, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(AccessPermission.READ, MegaShare.ACCESS_READ),
                Arguments.of(AccessPermission.READWRITE, MegaShare.ACCESS_READWRITE),
                Arguments.of(AccessPermission.FULL, MegaShare.ACCESS_FULL),
                Arguments.of(AccessPermission.OWNER, MegaShare.ACCESS_OWNER),
                Arguments.of(AccessPermission.UNKNOWN, -1),
                Arguments.of(AccessPermission.UNKNOWN, 10)
            )
        }
    }
}