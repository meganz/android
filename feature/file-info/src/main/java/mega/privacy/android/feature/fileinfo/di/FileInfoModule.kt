package mega.privacy.android.feature.fileinfo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.fileinfo.navigation.FileInfoFeatureGraph
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class FileInfoModule {

    @Provides
    @IntoSet
    fun provideFileInfoFeatureDestination(): FeatureDestination = FileInfoFeatureGraph()
}
