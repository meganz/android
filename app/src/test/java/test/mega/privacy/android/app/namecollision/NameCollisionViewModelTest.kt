package test.mega.privacy.android.app.namecollision

import com.jraska.livedata.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.namecollision.NameCollisionViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.TestSchedulerExtension
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class, TestSchedulerExtension::class])
internal class NameCollisionViewModelTest {
    private lateinit var underTest: NameCollisionViewModel

    private val getFileVersionsOption = mock<GetFileVersionsOption>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()

    private fun initUnderTest() {
        underTest = NameCollisionViewModel(
            getFileVersionsOption = getFileVersionsOption,
            getNameCollisionResultUseCase = mock(),
            uploadUseCase = mock(),
            legacyCopyNodeUseCase = mock(),
            monitorUserUpdates = monitorUserUpdates,
            getNodeUseCase = mock(),
            setCopyLatestTargetPathUseCase = mock(),
            setMoveLatestTargetPathUseCase = mock(),
            copyRequestMessageMapper = mock(),
            moveRequestMessageMapper = mock(),
            getNodeByFingerprintAndParentNodeUseCase = mock(),
            moveCollidedNodeUseCase = mock(),
            moveCollidedNodesUseCase = mock(),
        )
    }

    @Test
    internal fun `test that an exception in getFileVersionsOption is caught`() =
        withCoroutineExceptions {
            runTest {
                monitorUserUpdates.stub {
                    on { invoke() }.thenReturn(flowOf(UserChanges.DisableVersions))
                }

                getFileVersionsOption.stub {
                    onBlocking { invoke(true) }.thenAnswer { throw Exception("This test failed") }
                }

                initUnderTest()
                underTest.getFileVersioningInfo().test().assertNoValue()
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}