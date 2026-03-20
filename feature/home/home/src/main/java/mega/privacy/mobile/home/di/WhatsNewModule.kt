package mega.privacy.mobile.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import mega.privacy.mobile.home.presentation.whatsnew.WhatsNewDetail
import mega.privacy.mobile.home.presentation.whatsnew.detail.V16_1_WhatsNewDetail

@Module
@InstallIn(SingletonComponent::class)
class WhatsNewModule {
    @Provides
    @IntoMap
    @StringKey("16.1")
    fun provideV16_1detail(): WhatsNewDetail = V16_1_WhatsNewDetail
}