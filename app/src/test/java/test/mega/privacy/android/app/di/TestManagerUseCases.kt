package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.manager.ManagerUseCases
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetBackupsChildrenNodes
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolderUseCase
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.HasBackupsChildren
import nz.mega.sdk.MegaNode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ManagerUseCases::class],
    components = [ViewModelComponent::class, ServiceComponent::class]
)
@Module(includes = [TestGetNodeModule::class])
object TestManagerUseCases {

    @Provides
    fun provideMonitorGlobalUpdates() = mock<MonitorGlobalUpdates>()

    @Provides
    fun provideGetRootFolder() = mock<GetRootFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetRubbishBinFolder() = mock<GetRubbishBinFolderUseCase> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetNumUnreadUserAlerts() = mock<GetNumUnreadUserAlertsUseCase> {
        on { runBlocking { invoke() } }.thenReturn(0)
    }

    @Provides
    fun provideHasBackupsChildren() = mock<HasBackupsChildren> {
        onBlocking { invoke() }.thenReturn(false)
    }

    @Provides
    fun provideGetBackupsChildrenNodes() = mock<GetBackupsChildrenNodes> {
        onBlocking { invoke() }.thenReturn(flowOf(any()))
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
    fun provideGetBackupsNode() = mock<GetBackupsNode> {
        onBlocking { invoke() }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetRubbishBinChildren() = mock<GetRubbishBinChildren> {}
}
