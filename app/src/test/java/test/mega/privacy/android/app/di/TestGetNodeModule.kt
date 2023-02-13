package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.app.domain.usecase.CopyNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.OpenShareDialog
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.UpgradeSecurity
import nz.mega.sdk.MegaNode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [GetNodeModule::class],
    components = [ViewModelComponent::class, SingletonComponent::class]
)
@Module
object TestGetNodeModule {

    @Provides
    fun provideCopyNode() = mock<CopyNode> {
        onBlocking { invoke(any(), any(), any()) }.thenReturn(NodeId(1L))
    }

    @Provides
    fun provideGetChildrenNode() = mock<GetChildrenNode> {
        onBlocking { invoke(any(), any()) }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetNodeByHandle() = mock<GetNodeByHandle> {
        onBlocking { invoke(any()) }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetUnverifiedIncomingShares() = mock<GetUnverifiedIncomingShares>() {
        onBlocking { invoke(any()) }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetUnverifiedOutgoingShares() = mock<GetUnverifiedOutgoingShares>() {
        onBlocking { invoke(any()) }.thenReturn(emptyList())
    }

    @Provides
    fun provideOpenShareDialog() = mock<OpenShareDialog>()

    @Provides
    fun provideUpgradeSecurity() = mock<UpgradeSecurity>()
}
