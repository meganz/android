package mega.privacy.android.app.fragments.managerFragments.cu;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
public class CuItemSizeConfig {
    private final boolean smallGrid;
    private final int gridSize;
    private final int gridMargin;
    private final int icSelectedSize;
    private final int icSelectedMargin;
    private final int roundCornerRadius;
    private final int selectedPadding;

    public CuItemSizeConfig(boolean smallGrid, int gridSize, int gridMargin, int icSelectedSize,
                            int icSelectedMargin, int roundCornerRadius, int selectedPadding) {
        this.smallGrid = smallGrid;
        this.gridSize = gridSize;
        this.gridMargin = gridMargin;
        this.icSelectedSize = icSelectedSize;
        this.icSelectedMargin = icSelectedMargin;
        this.roundCornerRadius = roundCornerRadius;
        this.selectedPadding = selectedPadding;
    }

    public boolean isSmallGrid() {
        return smallGrid;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getGridMargin() {
        return gridMargin;
    }

    public int getIcSelectedSize() {
        return icSelectedSize;
    }

    public int getIcSelectedMargin() {
        return icSelectedMargin;
    }

    public int getRoundCornerRadius() {
        return roundCornerRadius;
    }

    public int getSelectedPadding() {
        return selectedPadding;
    }
}