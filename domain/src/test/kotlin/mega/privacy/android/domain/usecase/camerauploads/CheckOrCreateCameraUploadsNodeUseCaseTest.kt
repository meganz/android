package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetDefaultNodeHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckOrCreateCameraUploadsNodeUseCaseTest {

    private lateinit var underTest: CheckOrCreateCameraUploadsNodeUseCase

    private val isCameraUploadsNodeValidUseCase: IsCameraUploadsNodeValidUseCase = mock()
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase = mock()
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase = mock()
    private val createFolderNodeUseCase: CreateFolderNodeUseCase = mock()
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase = mock()
    private val setPrimaryNodeIdUseCase: SetPrimaryNodeIdUseCase = mock()
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase = mock()
    private val setSecondaryNodeIdUseCase: SetSecondaryNodeIdUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = CheckOrCreateCameraUploadsNodeUseCase(
            isCameraUploadsNodeValidUseCase = isCameraUploadsNodeValidUseCase,
            getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
            getDefaultNodeHandleUseCase = getDefaultNodeHandleUseCase,
            createFolderNodeUseCase = createFolderNodeUseCase,
            setupPrimaryFolderUseCase = setupPrimaryFolderUseCase,
            setPrimaryNodeIdUseCase = setPrimaryNodeIdUseCase,
            setupSecondaryFolderUseCase = setupSecondaryFolderUseCase,
            setSecondaryNodeIdUseCase = setSecondaryNodeIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getUploadFolderHandleUseCase,
            getDefaultNodeHandleUseCase,
            createFolderNodeUseCase,
            setupPrimaryFolderUseCase,
            setPrimaryNodeIdUseCase,
            setupSecondaryFolderUseCase,
            setSecondaryNodeIdUseCase,
        )
    }

    @ParameterizedTest(name = "when folderType is {0}")
    @EnumSource(CameraUploadFolderType::class)
    fun `test that the use case does not fetch the default folder if the upload node in local preferences is valid`(
        folderType: CameraUploadFolderType
    ) = runTest {
        val defaultFolderName = "defaultFolderName"
        val nodeId = NodeId(1234L)
        whenever(getUploadFolderHandleUseCase(folderType))
            .thenReturn(nodeId.longValue)
        whenever(isCameraUploadsNodeValidUseCase(nodeId)).thenReturn(true)

        underTest.invoke(defaultFolderName, folderType)

        verifyNoInteractions(getDefaultNodeHandleUseCase)
        verifyNoInteractions(createFolderNodeUseCase)
        when (folderType) {
            CameraUploadFolderType.Primary -> {
                verifyNoInteractions(setupPrimaryFolderUseCase)
                verifyNoInteractions(setPrimaryNodeIdUseCase)
            }

            CameraUploadFolderType.Secondary -> {
                verifyNoInteractions(setupSecondaryFolderUseCase)
                verifyNoInteractions(setSecondaryNodeIdUseCase)
            }
        }
    }

    @ParameterizedTest(name = "when folderType is {0}")
    @EnumSource(CameraUploadFolderType::class)
    fun `test that the use case set the default folder node in local preferences if the upload node in local preferences is not valid and the default folder node exists`(
        folderType: CameraUploadFolderType
    ) =
        runTest {
            val defaultFolderName = "defaultFolderName"
            val nodeId = NodeId(1234L)
            val defaultFolderNodeId = NodeId(1111L)
            whenever(getUploadFolderHandleUseCase(folderType))
                .thenReturn(nodeId.longValue)
            whenever(isCameraUploadsNodeValidUseCase(nodeId)).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(defaultFolderName)).thenReturn(defaultFolderNodeId.longValue)
            whenever(isCameraUploadsNodeValidUseCase(defaultFolderNodeId)).thenReturn(true)

            underTest.invoke(defaultFolderName, folderType)

            verifyNoInteractions(createFolderNodeUseCase)
            when (folderType) {
                CameraUploadFolderType.Primary -> {
                    verify(setPrimaryNodeIdUseCase).invoke(defaultFolderNodeId)
                    verifyNoInteractions(setupPrimaryFolderUseCase)
                }

                CameraUploadFolderType.Secondary -> {
                    verify(setSecondaryNodeIdUseCase).invoke(defaultFolderNodeId)
                    verifyNoInteractions(setupSecondaryFolderUseCase)
                }
            }
        }

    @ParameterizedTest(name = "when folderType is {0}")
    @EnumSource(CameraUploadFolderType::class)
    fun `test that the use case set the default folder node in local preferences if the upload node in local preferences is in rubbish bin or deleted and the default folder node exists`(
        folderType: CameraUploadFolderType
    ) =
        runTest {
            val defaultFolderName = "defaultFolderName"
            val folderNodeId = NodeId(1111L)
            val defaultFolderNodeId = NodeId(1234L)
            whenever(getUploadFolderHandleUseCase(folderType))
                .thenReturn(folderNodeId.longValue)
            whenever(isCameraUploadsNodeValidUseCase(folderNodeId)).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(defaultFolderName)).thenReturn(defaultFolderNodeId.longValue)
            whenever(isCameraUploadsNodeValidUseCase(defaultFolderNodeId)).thenReturn(true)

            underTest.invoke(defaultFolderName, folderType)

            verifyNoInteractions(createFolderNodeUseCase)
            when (folderType) {
                CameraUploadFolderType.Primary -> {
                    verify(setPrimaryNodeIdUseCase).invoke(defaultFolderNodeId)
                    verifyNoInteractions(setupPrimaryFolderUseCase)
                }

                CameraUploadFolderType.Secondary -> {
                    verify(setSecondaryNodeIdUseCase).invoke(defaultFolderNodeId)
                    verifyNoInteractions(setupSecondaryFolderUseCase)
                }
            }
        }

    @ParameterizedTest(name = "when folderType is {0}")
    @EnumSource(CameraUploadFolderType::class)
    fun `test that the use case create and set the default folder node in local preferences if the upload node in local preference is not valid and the default folder node is not valid`(
        folderType: CameraUploadFolderType
    ) =
        runTest {
            val defaultFolderName = "defaultFolderName"
            val folderNodeId = NodeId(1111L)
            val defaultFolderNodeId = NodeId(1234L)
            val createdDefaultFolderNodeId = NodeId(5678L)
            whenever(getUploadFolderHandleUseCase(folderType))
                .thenReturn(folderNodeId.longValue)
            whenever(isCameraUploadsNodeValidUseCase(folderNodeId)).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(defaultFolderName)).thenReturn(defaultFolderNodeId.longValue)
            whenever(isCameraUploadsNodeValidUseCase(defaultFolderNodeId)).thenReturn(false)
            whenever(createFolderNodeUseCase(defaultFolderName))
                .thenReturn(createdDefaultFolderNodeId)

            underTest.invoke(defaultFolderName, folderType)

            verify(createFolderNodeUseCase).invoke(defaultFolderName)
            when (folderType) {
                CameraUploadFolderType.Primary -> {
                    verify(setupPrimaryFolderUseCase).invoke(createdDefaultFolderNodeId.longValue)
                    verifyNoInteractions(setPrimaryNodeIdUseCase)
                }

                CameraUploadFolderType.Secondary -> {
                    verify(setupSecondaryFolderUseCase).invoke(createdDefaultFolderNodeId.longValue)
                    verifyNoInteractions(setSecondaryNodeIdUseCase)
                }
            }
        }
}

