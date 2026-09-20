package mega.privacy.android.app.di.node

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.shared.nodes.mapper.message.CopyRequestMessageMapper
import mega.privacy.android.shared.nodes.mapper.message.CopyRequestMessageMapperImpl

/**
 * Node Action Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NodeActionMapperModule {
    companion object {
        /**
         * Provide Copy Request Message Mapper
         */
        @Provides
        fun provideCopyRequestMessageMapper(@ApplicationContext context: Context): CopyRequestMessageMapper =
            CopyRequestMessageMapperImpl(context)
    }
}