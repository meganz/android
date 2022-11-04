package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.DefaultAddNodeType
import mega.privacy.android.domain.usecase.DefaultGetDeviceType
import mega.privacy.android.domain.usecase.DefaultGetFolderType
import mega.privacy.android.domain.usecase.DefaultHasAncestor
import mega.privacy.android.domain.usecase.GetDeviceType
import mega.privacy.android.domain.usecase.GetFolderType
import mega.privacy.android.domain.usecase.HasAncestor

/**
 * Module class regarding TypedNode
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GetTypedNodeModule {

    /**
     * Provide AddNodeType implementation
     */
    @Binds
    abstract fun bindAddNodeType(implementation: DefaultAddNodeType): AddNodeType

    /**
     * Provide GetFolderType implementation
     */
    @Binds
    abstract fun bindGetFolderType(implementation: DefaultGetFolderType): GetFolderType

    /**
     * Provide GetDeviceType implementation
     */
    @Binds
    abstract fun bindGetDeviceType(implementation: DefaultGetDeviceType): GetDeviceType

    /**
     * Provide HasAncestor implementation
     */
    @Binds
    abstract fun bindHasAncestor(implementation: DefaultHasAncestor): HasAncestor
}