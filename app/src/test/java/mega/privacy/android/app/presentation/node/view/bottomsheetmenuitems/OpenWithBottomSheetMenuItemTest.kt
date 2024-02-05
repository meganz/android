package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Context
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.OpenWithMenuAction
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFilePathUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenWithBottomSheetMenuItemTest {
    private val openWithMenuAction = OpenWithMenuAction()
    private val getFileUriUseCase = mock<GetFileUriUseCase>()
    private val getLocalFilePathUseCase = mock<GetNodePreviewFilePathUseCase>()
    private val httpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getStreamingUriStringForNode = mock<GetStreamingUriStringForNode>()
    private val isHttpServerRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()
    private val context = mock<Context>()
    private val scope = mock<CoroutineScope>()
    private val underTest = OpenWithBottomSheetMenuItem(
        openWithMenuAction,
        getFileUriUseCase,
        getLocalFilePathUseCase,
        httpServerStartUseCase,
        isHttpServerRunningUseCase,
        getStreamingUriStringForNode,
        snackBarHandler,
        context,
        scope,
    )

    @ParameterizedTest(name = "isNodeInRubbish {0} - accessPermission {1} - isInBackups {2} - node {3} - expected {4}")
    @MethodSource("provideTestParameters")
    fun `test that open with bottom sheet menu item visibility is correct`(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        expected: Boolean,
    ) = runTest {
        val result = underTest.shouldDisplay(
            isNodeInRubbish,
            accessPermission,
            isInBackups,
            node,
            true
        )
        assertEquals(expected, result)
    }

    private fun provideTestParameters() = Stream.of(
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn true },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.READWRITE,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
        Arguments.of(
            true,
            AccessPermission.OWNER,
            false,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            false
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFileNode> { on { isTakenDown } doReturn false },
            true
        ),
        Arguments.of(
            false,
            AccessPermission.OWNER,
            true,
            mock<TypedFolderNode> { on { isTakenDown } doReturn false },
            false
        ),
    )

    @Test
    fun `test that bottom sheet is dismissed if selected node is not file node`() {
        val node = mock<TypedFolderNode>()
        val onDismiss = mock<() -> Unit>()
        val actionHandler = { _: MenuAction, _: TypedNode -> }
        val navController = mock<NavHostController>()
        val onClickFunction =
            underTest.getOnClickFunction(node, onDismiss, actionHandler, navController)
        onClickFunction.invoke()
        verify(onDismiss).invoke()
        verifyNoInteractions(getLocalFilePathUseCase)
    }

}