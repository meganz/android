package mega.privacy.android.domain.entity.meeting

/**
 * Month week day item
 *
 * @property weekOfMonth        [WeekOfMonth]
 * @property weekDaysList       [Weekday] list
 */
data class MonthWeekDayItem(val weekOfMonth: WeekOfMonth, val weekDaysList: List<Weekday>)