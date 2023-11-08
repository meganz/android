package mega.privacy.android.data.mapper.apiserver

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.apiserver.ApiServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiServerMapperTest {

    private lateinit var underTest: ApiServerMapper

    @BeforeAll
    fun setup() {
        underTest = ApiServerMapper()
    }

    @ParameterizedTest(name = " if received value is {0}")
    @MethodSource("provideParameters")
    fun `test that api server mapper returns correctly`(apiValue: Int, apiServer: ApiServer) {
        Truth.assertThat(underTest(apiValue)).isEqualTo(apiServer)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(0, ApiServer.Production),
        Arguments.of(1, ApiServer.Staging),
        Arguments.of(2, ApiServer.Staging444),
        Arguments.of(3, ApiServer.Sandbox3),
        Arguments.of(4, ApiServer.Production),
        Arguments.of(-1, ApiServer.Production),
    )
}