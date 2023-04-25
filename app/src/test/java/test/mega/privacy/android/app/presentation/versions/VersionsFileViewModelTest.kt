package test.mega.privacy.android.app.presentation.versions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.versions.VersionsFileViewModel
import mega.privacy.android.domain.usecase.IsNodeInInbox
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [VersionsFileViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionsFileViewModelTest {

    private lateinit var underTest: VersionsFileViewModel

    private val isNodeInInbox = mock<IsNodeInInbox>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun reset() {
        underTest = VersionsFileViewModel(
            isNodeInInbox = isNodeInInbox,
        )
        reset(isNodeInInbox)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isNodeInBackups).isFalse()
        }
    }

    @Test
    fun `test that the node is not a backup node by default if the node handle is null`() =
        runTest {
            underTest.init(null)
            underTest.state.test {
                assertThat(awaitItem().isNodeInBackups).isFalse()
            }

            verifyNoInteractions(isNodeInInbox)
        }

    @ParameterizedTest(name = "is node in backups: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the node could be a backup node on initialization`(isNodeInBackups: Boolean) =
        runTest {
            whenever(isNodeInInbox(any())).thenReturn(isNodeInBackups)

            underTest.init(123456L)
            underTest.state.test {
                assertThat(awaitItem().isNodeInBackups).isEqualTo(isNodeInBackups)
            }
        }
}