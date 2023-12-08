package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AreCameraUploadsFolderInRubbishBinUseCaseTest {
    lateinit var underTest: AreCameraUploadsFoldersInRubbishBinUseCase

    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()

    private val primaryHandle = 11111L
    private val secondaryHandle = 22222L
    private val nodeHandle = 88888L

    @BeforeAll
    fun setUp() {
        underTest = AreCameraUploadsFoldersInRubbishBinUseCase(
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isSecondaryFolderEnabled,
            isNodeInRubbishOrDeletedUseCase,
        )
    }

    @Test
    fun `test that underTest returns true when node update with primary handle and primary folder is in rubbish bin`() =
        runTest {
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(primaryHandle)) },
                        listOf(NodeChanges.Attributes)
                    )
                )
            )

            whenever(isNodeInRubbishOrDeletedUseCase(primaryHandle)).thenReturn(true)
            whenever(isSecondaryFolderEnabled()).thenReturn(false)
            val actual = underTest(primaryHandle, secondaryHandle, nodeUpdate)
            val expected = true
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that underTest returns true when node update with secondary handle and primary folder is in rubbish bin`() =
        runTest {
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(primaryHandle)) },
                        listOf(NodeChanges.Attributes)
                    )
                )
            )

            whenever(isNodeInRubbishOrDeletedUseCase(secondaryHandle)).thenReturn(false)
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(isNodeInRubbishOrDeletedUseCase(primaryHandle)).thenReturn(true)
            val actual = underTest(primaryHandle, secondaryHandle, nodeUpdate)
            val expected = true
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that underTest returns false when node update is not the camera uploads primary or secondary folder`() =
        runTest {
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(nodeHandle)) },
                        listOf(NodeChanges.Attributes)
                    )
                )
            )

            val actual = underTest(primaryHandle, secondaryHandle, nodeUpdate)
            val expected = false
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that underTest returns false when node update is primary folder but not attribute change`() =
        runTest {
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(primaryHandle)) },
                        listOf()
                    )
                )
            )

            val actual = underTest(primaryHandle, secondaryHandle, nodeUpdate)
            val expected = false
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that underTest returns false when node update is secondary folder but not attribute change`() =
        runTest {
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(secondaryHandle)) },
                        listOf()
                    )
                )
            )

            val actual = underTest(primaryHandle, secondaryHandle, nodeUpdate)
            val expected = false
            assertThat(actual).isEqualTo(expected)
        }

}
