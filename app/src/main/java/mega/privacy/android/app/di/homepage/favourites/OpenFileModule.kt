package mega.privacy.android.app.di.homepage.favourites

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.utils.OpenFileHelper

@Module
@InstallIn(FragmentComponent::class)
abstract class OpenFileModule {
    @Binds
    abstract fun bindOpenFileWrapper(useCase: OpenFileHelper): OpenFileWrapper
}