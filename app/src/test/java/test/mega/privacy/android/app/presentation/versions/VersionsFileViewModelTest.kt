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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

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
    fun `test that the node is not a backup node by default when the node handle is null`() =
        runTest {
            underTest.init(null)
            underTest.state.test {
                assertThat(awaitItem().isNodeInBackups).isFalse()
            }

            verifyNoInteractions(isNodeInInbox)
        }

    @ParameterizedTest(name = "is node in backups: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the node can be a backup node when initialized`(isNodeInBackups: Boolean) =
        runTest {
            whenever(isNodeInInbox(any())).thenReturn(isNodeInBackups)

            underTest.init(123456L)
            underTest.state.test {
                assertThat(awaitItem().isNodeInBackups).isEqualTo(isNodeInBackups)
            }
        }

    @Test
    fun `test that the action bar delete button is hidden when there are no versions selected`() =
        runTest {
            whenever(isNodeInInbox(any())).thenReturn(false)
            underTest.init(123456L)
            val expected = underTest.showDeleteVersionsButton(
                selectedVersions = 0,
                isCurrentVersionSelected = false,
            )
            assertThat(expected).isFalse()
        }

    @ParameterizedTest(name = "when isNodeInBackups is {0} and isCurrentVersionSelected is {1}, show delete versions button is {2}")
    @MethodSource("provideDeleteParameters")
    fun `test that the visibility of the action bar delete button is handled when there are versions selected`(
        isNodeInBackups: Boolean,
        isCurrentVersionSelected: Boolean,
        showDeleteVersionsButton: Boolean,
    ) = runTest {
        whenever(isNodeInInbox(any())).thenReturn(isNodeInBackups)

        underTest.init(123456L)
        val expected = underTest.showDeleteVersionsButton(
            selectedVersions = 1,
            isCurrentVersionSelected = isCurrentVersionSelected,
        )
        assertThat(expected).isEqualTo(showDeleteVersionsButton)
    }

    /**
     * Provides parameters for the test concerning the "Delete" button visibility
     *
     * The parameters are ordered into:
     * 1. Is Node in Backups
     * 2. Is Current Version Selected
     * 3. Show Delete Versions Button
     */
    private fun provideDeleteParameters() = Stream.of(
        Arguments.of(true, true, false),
        Arguments.of(true, false, true),
        Arguments.of(false, true, true),
        Arguments.of(false, false, true),
    )

    @Test
    fun `test that the action bar revert button is hidden when there are no versions selected`() =
        runTest {
            whenever(isNodeInInbox(any())).thenReturn(false)
            underTest.init(123456L)
            val expected = underTest.showRevertVersionButton(
                selectedVersions = 0,
                isCurrentVersionSelected = false,
            )
            assertThat(expected).isFalse()
        }

    @ParameterizedTest(name = "when isNodeInBackups is {0} and isCurrentVersionSelected is {1}, show revert version button is {2}")
    @MethodSource("provideRevertParameters")
    fun `test that the visibility of the action bar revert button is handled when there is only one version selected`(
        isNodeInBackups: Boolean,
        isCurrentVersionSelected: Boolean,
        showRevertVersionButton: Boolean,
    ) = runTest {
        whenever(isNodeInInbox(any())).thenReturn(isNodeInBackups)

        underTest.init(123456L)
        val expected = underTest.showRevertVersionButton(
            selectedVersions = 1,
            isCurrentVersionSelected = isCurrentVersionSelected,
        )
        assertThat(expected).isEqualTo(showRevertVersionButton)
    }

    /**
     * Provides parameters for the test concerning the "Revert" button visibility
     *
     * The parameters are ordered into:
     * 1. Is Node in Backups
     * 2. Is Current Version Selected
     * 3. Show Revert Version Button
     */
    private fun provideRevertParameters() = Stream.of(
        Arguments.of(true, true, false),
        Arguments.of(true, false, false),
        Arguments.of(false, true, false),
        Arguments.of(false, false, true),
    )

    @Test
    fun `test that the action bar revert button is hidden when there are more than one version selected`() =
        runTest {
            whenever(isNodeInInbox(any())).thenReturn(false)

            underTest.init(123456L)
            val expected = underTest.showRevertVersionButton(
                selectedVersions = 2,
                isCurrentVersionSelected = false,
            )
            assertThat(expected).isFalse()
        }
}