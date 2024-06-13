package mega.privacy.android.app.presentation.cancelaccountplan.view


/**
 * Represents a cell in a table.
 */
sealed class TableCell {

    /**
     * Represents a cell with text.
     *
     * @property text The text to display.
     * @property style The style of the text.
     * @property cellAlignment The alignment of the cell.
     */
    data class TextCell(
        val text: String,
        val style: TextCellStyle,
        val cellAlignment: CellAlignment,
    ) : TableCell()

    /**
     * Represents a cell with an icon.
     *
     * @property iconResId The resource id of the icon.
     */
    data class IconCell(val iconResId: Int) : TableCell()

    /**
     *  Enum class for text cell style.
     */
    enum class TextCellStyle {
        /**
         * Represents a header cell.
         */
        Header,

        /**
         * Represents a SubHeader cell.
         */
        SubHeader,

        /**
         * Represents a normal cell.
         */
        Normal
    }

    /**
     * Enum class for cell alignment.
     */
    enum class CellAlignment {
        /**
         * Cell aligned to the start
         */
        Start,

        /**
         * Represents a cell that is centered
         */
        Center,

        /**
         * Cell aligned to the end
         */
        End
    }
}