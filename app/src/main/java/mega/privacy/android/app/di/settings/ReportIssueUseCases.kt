package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class ReportIssueUseCases{
    @Binds
    abstract fun bindSubmitIssue(implementation: DefaultSubmitIssue): SubmitIssue

    @Binds
    abstract fun bindFormatSupportTicket(implementation: DefaultFormatSupportTicket): FormatSupportTicket

    @Binds
    abstract fun bindCreateSupportTicket(implementation: DefaultCreateSupportTicket): CreateSupportTicket

}