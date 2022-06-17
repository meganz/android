package test.mega.privacy.android.app.di

import dagger.Provides
import dagger.Module
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.manager.ManagerUseCases
import mega.privacy.android.app.domain.usecase.DefaultGetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import nz.mega.sdk.MegaNode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ManagerUseCases::class],
    components = [ViewModelComponent::class]
)
@Module
object TestManagerUseCases {

    @Provides
    fun bindMonitorGlobalUpdates() = mock<MonitorGlobalUpdates>() {
        on { run { invoke() } }.thenReturn(flowOf(any()))
    }

    @Provides
    fun bindMonitorNodeUpdates() = mock<MonitorNodeUpdates> {
        on { run { invoke() } }.thenReturn(flowOf(any()))
    }

    @Provides
    fun bindRubbishBinChildrenNode(useCase: DefaultGetRubbishBinChildrenNode) =
        mock<GetRubbishBinChildrenNode> {
            on { runBlocking { invoke(0) } }.thenReturn(emptyList())
        }

    @Provides
    fun bindBrowserChildrenNode(useCase: DefaultGetBrowserChildrenNode) =
        mock<GetBrowserChildrenNode> {
            on { runBlocking { invoke(0) } }.thenReturn(emptyList())
        }

    @Provides
    fun bindGetRootFolder() = mock<GetRootFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun bindGetRubbishBinFolder() = mock<GetRubbishBinFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun bindGetChildrenNode() = mock<GetChildrenNode> {
        on { runBlocking { invoke(any()) } }.thenReturn(emptyList())
    }

    @Provides
    fun bindGetNodeByHandle() = mock<GetNodeByHandle> {
        on { runBlocking { invoke(any()) } }.thenReturn(MegaNode())
    }

    @Provides
    fun bindGetNumUnreadUserAlerts() = mock<GetNumUnreadUserAlerts> {
        on { runBlocking { invoke() } }.thenReturn(0)
    }
}