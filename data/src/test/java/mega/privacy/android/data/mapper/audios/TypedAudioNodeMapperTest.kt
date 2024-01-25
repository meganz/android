package mega.privacy.android.data.mapper.audios

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.FileNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import javax.inject.Inject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypedAudioNodeMapperTest @Inject constructor() {
    private lateinit var underTest: TypedAudioNodeMapper

    @BeforeAll
    fun setUp() {
        underTest = TypedAudioNodeMapper()
    }

    @Test
    fun `test that TypedAudioNodeMapper can be mapped correctly`() {
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