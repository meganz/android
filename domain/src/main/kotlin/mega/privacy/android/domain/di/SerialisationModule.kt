package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.chat.messages.serialisation.typedMessageSerialisationModule

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
                serializersModule = typedMessageSerialisationModule
            }
        }
    }
}