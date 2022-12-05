package mega.privacy.android.app.di.chat

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.usecase.DefaultGetMeetings
import mega.privacy.android.domain.usecase.GetMeetings

/**
 * Provide implementation for use cases related with chat rooms.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class ChatRoomsUseCases {

    @Binds
    abstract fun bindGetMeetings(useCase: DefaultGetMeetings): GetMeetings
}
