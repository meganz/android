package mega.privacy.android.data.mapper.videos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.FileNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import javax.inject.Inject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypedVideoNodeMapperTest @Inject constructor() {
    private lateinit var underTest: TypedVideoNodeMapper

    @BeforeAll
    fun setUp() {
        underTest = TypedVideoNodeMapper()
    }

    @Test
    fun `test that VideoNodeMapper can be mapped correctly`() {
        val expectedDuration = 1000
        val expectedFileNode = mock<FileNode>()

        underTest(
            fileNode = expectedFileNode,
            duration = expectedDuration,
        ).let {
            assertThat(it.duration.inWholeSeconds).isEqualTo(expectedDuration)
        }
    }
}