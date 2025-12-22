package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import java.util.Calendar
import javax.inject.Inject

class GetCalendarUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    operator fun invoke(): Calendar = environmentRepository.getCalendar()
}
