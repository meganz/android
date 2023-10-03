package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [BackupInfoUserAgentMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoUserAgentMapperTest {
    private lateinit var underTest: BackupInfoUserAgentMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoUserAgentMapper()
    }

    @ParameterizedTest(name = "when the sdk user agent is \"{0}\", then the backup info user agent is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(
        sdkUserAgent: String?,
        expectedResult: BackupInfoUserAgent,
    ) {
        assertThat(underTest(sdkUserAgent)).isEqualTo(expectedResult)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of("MEGAAndroid/123.456 MEGAEnv/QA", BackupInfoUserAgent.ANDROID),
        Arguments.of("MEGAiOS/123.456 MEGAEnv/QA", BackupInfoUserAgent.IPHONE),
        Arguments.of("MEGAsync Linux V1", BackupInfoUserAgent.LINUX),
        Arguments.of("MEGAsync FreeBSD V2", BackupInfoUserAgent.LINUX),
        Arguments.of("MEGAsync NetBSD V3", BackupInfoUserAgent.LINUX),
        Arguments.of("MEGAsync OpenBSD V4", BackupInfoUserAgent.LINUX),
        Arguments.of("MEGAsync Sunos V5", BackupInfoUserAgent.LINUX),
        Arguments.of("MEGAsync Gentoo V6", BackupInfoUserAgent.LINUX),
        Arguments.of("MEGAsync Windows V7", BackupInfoUserAgent.WINDOWS),
        Arguments.of("MEGAsync Mac V8", BackupInfoUserAgent.MAC),
        Arguments.of("MEGAsync Darwin V9", BackupInfoUserAgent.MAC),
        Arguments.of("MEGAAndroider", BackupInfoUserAgent.UNKNOWN),
        Arguments.of("MEGAiOSer", BackupInfoUserAgent.UNKNOWN),
        Arguments.of("MEGAsyncer", BackupInfoUserAgent.UNKNOWN),
        Arguments.of("MEGAsync Echo V10", BackupInfoUserAgent.UNKNOWN),
        Arguments.of("Different User Agent", BackupInfoUserAgent.UNKNOWN),
        Arguments.of("", BackupInfoUserAgent.UNKNOWN),
        Arguments.of(null, BackupInfoUserAgent.UNKNOWN),
    )
}