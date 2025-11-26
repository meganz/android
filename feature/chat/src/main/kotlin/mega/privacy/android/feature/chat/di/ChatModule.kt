package mega.privacy.android.feature.chat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.chat.navigation.ChatFeatureDestination
import mega.privacy.android.feature.chat.navigation.ChatsDeepLinkHandler
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler

@Module
@InstallIn(SingletonComponent::class)
class ChatModule {
    @Provides
    @IntoSet
    fun provideChatsDeepLinkHandler(handler: ChatsDeepLinkHandler): DeepLinkHandler = handler

    @Provides
    @IntoSet
    fun provideNodeComponentsFeatureDestination(): FeatureDestination = ChatFeatureDestination()
}