package mega.privacy.android.app.fragments.homepage.photos

import mega.privacy.android.app.gallery.data.GalleryCard

/**
 * Handle the click event on GalleryCard.
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
    fun getClickedCard(position: Int, handle: Long, cards: List<GalleryCard>?): GalleryCard? {
        if (cards == null) {
            return null
        }

        if (position < 0 || position > cards.size - 1) {
            return null
        }

        var card: GalleryCard? = cards[position]
        if (handle != card!!.id) {
            card = null
            for (c in cards) {
                if (c.id == handle) {
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
     * @return A month card index corresponding to the year clicked, current month index. If not exists,
     * the closest month index to the current.
     */
    fun yearClicked(
            position: Int,
            card: GalleryCard,
            months: List<GalleryCard>?,
            years: List<GalleryCard>?
    ): Int {
        val yearCard = getClickedCard(position, card.id, years) ?: return 0
        val monthCards = months ?: return 0

        val cardYear = yearCard.localDate.year

        for (i in monthCards.indices) {
            val nextLocalDate = monthCards[i].localDate
            if (nextLocalDate.year == cardYear) {
                //Year clicked, current month. If not exists, the closest month behind the current.
                if (i == 0 || monthCards[i - 1].localDate.year != cardYear) {
                    return i
                }
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
     * @return A day card index corresponding to the month of the year clicked, current day index. If not exists,
     * the closest day index to the current.
     */
    fun monthClicked(
            position: Int,
            card: GalleryCard,
            days: List<GalleryCard>?,
            months: List<GalleryCard>?
    ): Int {
        val monthCard = getClickedCard(position, card.id, months) ?: return 0
        val dayCards = days ?: return 0

        val cardLocalDate = monthCard.localDate
        val cardMonth = cardLocalDate.monthValue
        val cardYear = cardLocalDate.year

        for (i in dayCards.indices) {
            val nextLocalDate = dayCards[i].localDate
            val nextMonth = nextLocalDate.monthValue
            val nextYear = nextLocalDate.year

            if (nextYear == cardYear && nextMonth == cardMonth) {
                //Month of year clicked, current day. If not exists, the closest day behind the current.
                if (i == 0 || dayCards[i - 1].localDate.monthValue != cardMonth) {
                    return i
                }
            }
        }
        return dayCards.size - 1
    }
}