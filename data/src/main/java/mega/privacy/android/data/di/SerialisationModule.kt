package mega.privacy.android.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus
import mega.privacy.android.data.database.entity.chat.serialisation.messageInfoSerialisationModule
import mega.privacy.android.domain.entity.chat.messages.serialisation.typedMessageSerialisationModule
import mega.privacy.android.domain.entity.node.serialisation.nodeSerialisationModule

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SerialisationModule {

    companion object {
        /**
         * Provide json
         *
         * To add more serialisation modules, simply use the "+" operator. eg.
         *          return Json {
         *             serializersModule = typedMessageSerialisationModule + newModule
         *         }
         *
         * @return the kotlin serialisation Json class
         */
        @Provides
        fun provideJson(): Json {
            return Json {
                serializersModule =
                    typedMessageSerialisationModule +
                            messageInfoSerialisationModule +
                            nodeSerialisationModule
            }
        }
    }
}