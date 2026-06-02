package mega.privacy.android.feature.videoeditor.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.feature.videoeditor.navigation.VideoEditorFeatureGraph

@Module
@InstallIn(SingletonComponent::class)
class VideoEditorModule {

    @Provides
    @IntoSet
    fun provideVideoEditorFeatureDestination(): FeatureDestination = VideoEditorFeatureGraph()
}
