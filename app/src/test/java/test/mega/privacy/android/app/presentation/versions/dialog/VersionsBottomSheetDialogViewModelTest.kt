package test.mega.privacy.android.app.presentation.versions.dialog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.versions.dialog.VersionsBottomSheetDialogViewModel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.IsNodeInInboxUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [VersionsBottomSheetDialogViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionsBottomSheetDialogViewModelTest {

    private lateinit var underTest: VersionsBottomSheetDialogViewModel

    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val isNodeInInboxUseCase = mock<IsNodeInInboxUseCase>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun reset() {
        reset(getNodeAccessPermission, getNodeByHandle, isNodeInInboxUseCase)
    }

    fun initViewModel(nodeHandle: Long?, selectedNodePosition: Int, versionsCount: Int) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                VersionsBottomSheetDialogFragment.PARAM_NODE_HANDLE to nodeHandle,
                VersionsBottomSheetDialogFragment.PARAM_SELECTED_POSITION to selectedNodePosition,
                VersionsBottomSheetDialogFragment.PARAM_VERSIONS_COUNT to versionsCount,
            )
        )
        underTest = VersionsBottomSheetDialogViewModel(
            getNodeAccessPermission = getNodeAccessPermission,
            getNodeByHandle = getNodeByHandle,
            isNodeInInboxUseCase = isNodeInInboxUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned if the node handle is null`() = runTest {
        initViewModel(
            nodeHandle = null,
            selectedNodePosition = 0,
            versionsCount = 1,
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.canDeleteVersion).isFalse()
            assertThat(state.canRevertVersion).isFalse()
            assertThat(state.node).isNull()
        }

        verifyNoInteractions(getNodeAccessPermission, getNodeByHandle, isNodeInInboxUseCase)
    }

    @Test
    fun `test that the node is retrieved`() = runTest {
        val testNode = mock<MegaNode>()

        whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(mock())
        whenever(getNodeByHandle(any())).thenReturn(testNode)
        whenever(isNodeInInboxUseCase(any())).thenReturn(any())

        initViewModel(
            nodeHandle = 123456L,
            selectedNodePosition = 0,
            versionsCount = 1,
        )
        underTest.state.test {
            assertThat(awaitItem().node).isEqualTo(testNode)
        }
    }

    @Test
    fun `test that a non-backup node version cannot be deleted if it is the only version`() =
        runTest {
            whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())
            whenever(isNodeInInboxUseCase(any())).thenReturn(false)

            initViewModel(
                nodeHandle = 123456,
                selectedNodePosition = 0,
                versionsCount = 1,
            )
            underTest.state.test {
                assertThat(awaitItem().canDeleteVersion).isFalse()
            }
        }

    @ParameterizedTest(name = "access permission: {0}")
    @EnumSource(AccessPermission::class)
    fun `test that a non-backup node version could be deleted when there is more than one version`(
        accessPermission: AccessPermission,
    ) = runTest {
        whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(accessPermission)
        whenever(getNodeByHandle(any())).thenReturn(mock())
        whenever(isNodeInInboxUseCase(any())).thenReturn(false)

        initViewModel(
            nodeHandle = 123456,
            selectedNodePosition = 0,
            versionsCount = 3,
        )

        val expected = isDeleteAllowed(accessPermission)
        underTest.state.test {
            assertThat(awaitItem().canDeleteVersion).isEqualTo(expected)
        }
    }

    @Test
    fun `test that the current version backup node cannot be deleted`() = runTest {
        whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(mock())
        whenever(getNodeByHandle(any())).thenReturn(mock())
        whenever(isNodeInInboxUseCase(any())).thenReturn(true)

        initViewModel(
            nodeHandle = 123456L,
            selectedNodePosition = 0,
            versionsCount = 1,
        )
        underTest.state.test {
            assertThat(awaitItem().canDeleteVersion).isFalse()
        }
    }

    @TestFactory
    fun `test that a previous version backup node could be deleted`() =
        generateTestDeleteOptionData().map { (accessPermission, isDeleteAllowed) ->
            dynamicTest(
                "when accessPermission is $accessPermission, " +
                        "show delete option is $isDeleteAllowed"
            ) {
                runTest {
                    whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(accessPermission)
                    whenever(getNodeByHandle(any())).thenReturn(mock())
                    whenever(isNodeInInboxUseCase(any())).thenReturn(false)

                    initViewModel(
                        nodeHandle = 123456L,
                        selectedNodePosition = 1,
                        versionsCount = 2,
                    )
                    underTest.state.test {
                        assertThat(awaitItem().canDeleteVersion).isEqualTo(isDeleteAllowed)
                    }
                }
            }
        }

    @ParameterizedTest(name = "is node in inbox: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the current version node cannot be reverted`(isNodeInInbox: Boolean) = runTest {
        whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(mock())
        whenever(getNodeByHandle(any())).thenReturn(mock())
        whenever(isNodeInInboxUseCase(any())).thenReturn(isNodeInInbox)

        initViewModel(
            nodeHandle = 123456L,
            selectedNodePosition = 0,
            versionsCount = 1,
        )
        underTest.state.test {
            assertThat(awaitItem().canRevertVersion).isEqualTo(false)
        }
    }

    @TestFactory
    fun `test that a previous version non-backup node could be reverted`() =
        generateTestRevertOptionData().map { (accessPermission, isRevertAllowed) ->
            dynamicTest(
                "when accessPermission is $accessPermission, " +
                        "show revert option is $isRevertAllowed"
            ) {
                runTest {
                    whenever(getNodeAccessPermission(NodeId(any()))).thenReturn(accessPermission)
                    whenever(getNodeByHandle(any())).thenReturn(mock())
                    whenever(isNodeInInboxUseCase(any())).thenReturn(false)

                    initViewModel(
                        nodeHandle = 123456L,
                        selectedNodePosition = 1,
                        versionsCount = 2,
                    )
                    underTest.state.test {
                        assertThat(awaitItem().canRevertVersion).isEqualTo(isRevertAllowed)
                    }
                }
            }
        }

    private fun generateTestDeleteOptionData(): Map<AccessPermission?, Boolean> {
        val map = mutableMapOf<AccessPermission?, Boolean>().also {
            it[null] = isDeleteAllowed(accessPermission = null)
        }
        enumValues<AccessPermission>().forEach { accessPermission ->
            map[accessPermission] = isDeleteAllowed(accessPermission = accessPermission)
        }
        return map
    }

    private fun isDeleteAllowed(accessPermission: AccessPermission?): Boolean =
        accessPermission in listOf(AccessPermission.FULL, AccessPermission.OWNER)

    private fun generateTestRevertOptionData(): Map<AccessPermission?, Boolean> {
        val map = mutableMapOf<AccessPermission?, Boolean>().also {
            it[null] = isRevertAccessPermissionEligible(
                accessPermission = null,
            )
        }
        enumValues<AccessPermission>().forEach { accessPermission ->
            map[accessPermission] = isRevertAccessPermissionEligible(
                accessPermission = accessPermission,
            )
        }
        return map
    }

    private fun isRevertAccessPermissionEligible(
        accessPermission: AccessPermission?,
    ) = accessPermission in listOf(
        AccessPermission.READWRITE,
        AccessPermission.FULL,
        AccessPermission.OWNER,
    )
}