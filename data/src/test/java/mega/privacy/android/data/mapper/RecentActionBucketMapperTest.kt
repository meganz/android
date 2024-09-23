package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaRecentActionBucket
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class RecentActionBucketMapperTest {
    private lateinit var underTest: RecentActionBucketMapper

    private val nodeMapper = mock<NodeMapper>()

    private val recentActionBucket = mock<MegaRecentActionBucket> {
        on { timestamp }.thenReturn(12L)
        on { userEmail }.thenReturn("something@email.com")
        on { parentHandle }.thenReturn(1L)
        on { isUpdate }.thenReturn(true)
        on { isMedia }.thenReturn(false)
    }

    @Before
    fun setUp() {
        underTest = RecentActionBucketMapper(nodeMapper)
    }

    @Test
    fun `test that mapper returns correct value`() = runTest {
        val actual = underTest.invoke(
            recentActionBucket,
            listOf(mock(), mock())
        )
        assertThat(actual).isInstanceOf(RecentActionBucketUnTyped::class.java)
        assertThat(actual.timestamp).isEqualTo(recentActionBucket.timestamp)
        assertThat(actual.userEmail).isEqualTo(recentActionBucket.userEmail)
        assertThat(actual.parentNodeId.longValue).isEqualTo(recentActionBucket.parentHandle)
        assertThat(actual.isUpdate).isEqualTo(recentActionBucket.isUpdate)
        assertThat(actual.isMedia).isEqualTo(recentActionBucket.isMedia)
        assertThat(actual.nodes.size).isEqualTo(2)
    }
}
