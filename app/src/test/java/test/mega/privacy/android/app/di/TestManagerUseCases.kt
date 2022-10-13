package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.manager.ManagerUseCases
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.HasInboxChildren
import nz.mega.sdk.MegaNode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ManagerUseCases::class],
    components = [ViewModelComponent::class]
)
@Module(includes = [TestGetNodeModule::class])
object TestManagerUseCases {

    @Provides
    fun provideMonitorGlobalUpdates() = mock<MonitorGlobalUpdates>()

    @Provides
    fun provideMonitorNodeUpdates() = mock<MonitorNodeUpdates> {
        on { run { invoke() } }.thenReturn(flowOf(any()))
    }

    @Provides
    fun provideRubbishBinChildrenNode() = mock<GetRubbishBinChildrenNode> {
        on { runBlocking { invoke(0) } }.thenReturn(emptyList())
    }

    @Provides
    fun provideBrowserChildrenNode() = mock<GetBrowserChildrenNode> {
        on { runBlocking { invoke(0) } }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetRootFolder() = mock<GetRootFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetRubbishBinFolder() = mock<GetRubbishBinFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetNumUnreadUserAlerts() = mock<GetNumUnreadUserAlerts> {
        on { runBlocking { invoke() } }.thenReturn(0)
    }

    @Provides
    fun provideHasInboxChildren() = mock<HasInboxChildren> {
        onBlocking { invoke() }.thenReturn(false)
    }

    @Provides
    fun provideGetIncomingSharesNode() = mock<GetIncomingSharesChildrenNode> {
        onBlocking { invoke(any()) }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetOutgoingSharesNode() = mock<GetOutgoingSharesChildrenNode> {
        onBlocking { invoke(any()) }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetPublicLinks() = mock<GetPublicLinks> {
        onBlocking { invoke(any()) }.thenReturn(emptyList())
    }

    @Provides
    fun provideAuthorizeNode() = mock<AuthorizeNode> {
        onBlocking { invoke(any()) }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetParentNodeHandle() = mock<GetParentNodeHandle> {}

    @Provides
    fun provideGetInboxNode() = mock<GetInboxNode> {
        onBlocking { invoke() }.thenReturn(MegaNode())
    }
}