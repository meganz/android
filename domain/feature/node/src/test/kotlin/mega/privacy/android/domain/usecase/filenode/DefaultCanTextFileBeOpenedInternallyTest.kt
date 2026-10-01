package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

internal class DefaultCanTextFileBeOpenedInternallyTest {
    private lateinit var underTest: CanTextFileBeOpenedInternally

    @Before
    fun setUp() {
        underTest = DefaultCanTextFileBeOpenedInternally()
    }

    @Test
    fun `test that nodes smaller than the max size returns true`() = runTest {
        val node = mock<TypedFileNode> {
            on { size }.thenReturn(DefaultCanTextFileBeOpenedInternally.maxOpenableSizeBytes - 1)
        }
        assertThat(underTest(node)).isTrue()
    }

    @Test
    fun `test that nodes exactly the same as the max size returns true`() = runTest {
        val node = mock<TypedFileNode> {
            on { size }.thenReturn(DefaultCanTextFileBeOpenedInternally.maxOpenableSizeBytes)
        }
        assertThat(underTest(node)).isTrue()
    }

    @Test
    fun `test that nodes bigger than the max size returns false`() = runTest {
        val node = mock<TypedFileNode> {
            on { size }.thenReturn(DefaultCanTextFileBeOpenedInternally.maxOpenableSizeBytes + 1)
        }
        assertThat(underTest(node)).isFalse()
    }
}