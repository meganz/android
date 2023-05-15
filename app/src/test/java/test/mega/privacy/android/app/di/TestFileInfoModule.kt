package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.domain.di.ViewTypeModule
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [ViewTypeModule::class],
    components = [SingletonComponent::class],
)
object TestFileInfoModule {
    @Provides
    fun provideCheckNameCollision(): CheckNameCollision = mock()

    @Provides
    fun provideMoveNodeUseCase(): MoveNodeUseCase = mock()

    @Provides
    fun provideMoveNodeToRubbishByHandle(): MoveNodeToRubbishByHandle = mock()

    @Provides
    fun provideDeleteNodeByHandle(): DeleteNodeByHandle = mock()

    @Provides
    fun providesDelete(): DeleteNodeVersionsByHandle = mock()

    @Provides
    fun providesGetNodeLocationInfo(): GetNodeLocationInfo = mock()
}
