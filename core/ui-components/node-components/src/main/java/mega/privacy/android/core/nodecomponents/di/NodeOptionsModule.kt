package mega.privacy.android.core.nodecomponents.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.core.nodecomponents.menu.provider.BackupsMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.provider.CloudDriveMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.provider.IncomingSharesMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.provider.LinksMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.provider.NodeMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistryImpl
import mega.privacy.android.core.nodecomponents.menu.provider.OutgoingSharesMenuOptionsProvider
import mega.privacy.android.core.nodecomponents.menu.provider.RubbishBinMenuOptionsProvider

/**
 * Hilt module that binds all NodeOptionsProvider implementations and the registry.
 * This allows the NodeOptionsRegistry to automatically discover
 * all available providers through dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NodeOptionsModule {

    @Binds
    abstract fun bindNodeMenuProviderRegistry(
        nodeOptionsRegistryImpl: NodeMenuProviderRegistryImpl,
    ): NodeMenuProviderRegistry

    @Binds
    @IntoSet
    abstract fun bindCloudDriveOptionsProvider(
        cloudDriveOptionsProvider: CloudDriveMenuOptionsProvider,
    ): NodeMenuOptionsProvider

    @Binds
    @IntoSet
    abstract fun bindRubbishBinOptionsProvider(
        rubbishBinOptionsProvider: RubbishBinMenuOptionsProvider,
    ): NodeMenuOptionsProvider

    @Binds
    @IntoSet
    abstract fun bindIncomingSharesOptionsProvider(
        incomingSharesOptionsProvider: IncomingSharesMenuOptionsProvider,
    ): NodeMenuOptionsProvider

    @Binds
    @IntoSet
    abstract fun bindOutgoingSharesOptionsProvider(
        outgoingSharesOptionsProvider: OutgoingSharesMenuOptionsProvider,
    ): NodeMenuOptionsProvider

    @Binds
    @IntoSet
    abstract fun bindLinksOptionsProvider(
        linksOptionsProvider: LinksMenuOptionsProvider,
    ): NodeMenuOptionsProvider

    @Binds
    @IntoSet
    abstract fun bindBackupsOptionsProvider(
        backupsOptionsProvider: BackupsMenuOptionsProvider,
    ): NodeMenuOptionsProvider
}
