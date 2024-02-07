package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.usecase.CreateSupportTicket
import mega.privacy.android.domain.usecase.DefaultCreateSupportTicket
import mega.privacy.android.domain.usecase.DefaultFormatSupportTicket
import mega.privacy.android.domain.usecase.FormatSupportTicket

@Module
@InstallIn(ViewModelComponent::class)
abstract class ReportIssueUseCases {

    @Binds
    abstract fun bindFormatSupportTicket(implementation: DefaultFormatSupportTicket): FormatSupportTicket

    @Binds
    abstract fun bindCreateSupportTicket(implementation: DefaultCreateSupportTicket): CreateSupportTicket

}