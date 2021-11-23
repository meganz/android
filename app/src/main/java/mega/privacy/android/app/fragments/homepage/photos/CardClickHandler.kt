package mega.privacy.android.app.fragments.homepage.photos

import mega.privacy.android.app.fragments.managerFragments.cu.CUCard
import java.time.LocalDate

/**
 * Handle the click event on CUCard.
 */
object CardClickHandler {

    /**
     * Checks a clicked card, if it is in the provided list and if is in right position.
     *
     * @param position Clicked position in the list.
     * @param handle   Identifier of the card node.
     * @param cards    List of cards to check.
     * @return The checked card if found, null otherwise.
     */
    fun getClickedCard(position: Int, handle: Long, cards: List<CUCard>?): CUCard? {
        if (cards == null) {
            return null
        }
        if (position < 0 || position > cards.size - 1)
            return null
        var card: CUCard? = cards[position]
        if (handle != card!!.node.handle) {
            card = null
            for (c in cards) {
                if (c.node.handle == handle) {
                    card = c
                    break
                }
            }
        }

        return card
    }

    /**
     * Checks the clicked year card and gets the month card to show after click on a year card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked year card.
     * @param months Months cards list.
     * @param years Years cards list.
     * @return A month card corresponding to the year clicked, current month. If not exists,
     * the closest month to the current.
     */
    fun yearClicked(position: Int, card: CUCard, months: List<CUCard>?, years: List<CUCard>?): Int {
        val yearCard = getClickedCard(position, card.node.handle, years) ?: return 0
        val monthCards = months ?: return 0

        val cardYear = yearCard.localDate.year
        val currentMonth = LocalDate.now().monthValue
        for (i in monthCards.indices) {
            val nextLocalDate = monthCards[i].localDate
            val nextMonth = nextLocalDate.monthValue
            if (nextLocalDate.year == cardYear && nextMonth <= currentMonth) {
                //Year clicked, current month. If not exists, the closest month behind the current.
                if (i == 0 || nextMonth == currentMonth || monthCards[i - 1].localDate.year != cardYear) {
                    return i
                }
                val previousMonth = monthCards[i - 1].localDate.monthValue.toLong()

                //The closest month to the current
                return if (previousMonth - currentMonth <= currentMonth - nextMonth) i - 1 else i
            }
        }

        //No month equal or behind the current found, then return the latest month.
        return monthCards.size - 1
    }

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked month card.
     * @param days Days cards list.
     * @param months Months cards list.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    fun monthClicked(position: Int, card: CUCard, days: List<CUCard>?, months: List<CUCard>?): Int {
        val monthCard = getClickedCard(position, card.node.handle, months) ?: return 0
        val dayCards = days ?: return 0

        val cardLocalDate = monthCard.localDate
        val cardMonth = cardLocalDate.monthValue
        val cardYear = cardLocalDate.year
        val currentDay = LocalDate.now().dayOfMonth
        var dayPosition = 0
        for (i in dayCards.indices) {
            val nextLocalDate = dayCards[i].localDate
            val nextDay = nextLocalDate.dayOfMonth
            val nextMonth = nextLocalDate.monthValue
            val nextYear = nextLocalDate.year
            if (nextYear == cardYear && nextMonth == cardMonth) {
                dayPosition = if (nextDay <= currentDay) {
                    //Month of year clicked, current day. If not exists, the closest day behind the current.
                    if (i == 0 || nextDay == currentDay || dayCards[i - 1].localDate.monthValue != cardMonth) {
                        return i
                    }
                    val previousDay = dayCards[i - 1].localDate.dayOfMonth

                    //The closest day to the current
                    return if (previousDay - currentDay <= currentDay - nextDay) i - 1 else i
                } else {
                    //Save the closest day above the current in case there is no day of month behind the current.
                    i
                }
            }
        }
        return dayPosition
    }
}