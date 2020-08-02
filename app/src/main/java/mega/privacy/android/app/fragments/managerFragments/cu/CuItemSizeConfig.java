package mega.privacy.android.app.fragments.managerFragments.cu;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
public class CuItemSizeConfig {
    private final boolean mSmallGrid;
    private final int mGridSize;
    private final int mGridMargin;
    private final int mIcSelectedSize;
    private final int mIcSelectedMargin;
    private final int mRoundCornerRadius;
    private final int mSelectedPadding;

    public CuItemSizeConfig(boolean smallGrid, int gridSize, int gridMargin, int icSelectedSize,
            int icSelectedMargin, int roundCornerRadius, int selectedPadding) {
        mSmallGrid = smallGrid;
        mGridSize = gridSize;
        mGridMargin = gridMargin;
        mIcSelectedSize = icSelectedSize;
        mIcSelectedMargin = icSelectedMargin;
        mRoundCornerRadius = roundCornerRadius;
        mSelectedPadding = selectedPadding;
    }

    public boolean isSmallGrid() {
        return mSmallGrid;
    }

    public int getGridSize() {
        return mGridSize;
    }

    public int getGridMargin() {
        return mGridMargin;
    }

    public int getIcSelectedSize() {
        return mIcSelectedSize;
    }

    public int getIcSelectedMargin() {
        return mIcSelectedMargin;
    }

    public int getRoundCornerRadius() {
        return mRoundCornerRadius;
    }

    public int getSelectedPadding() {
        return mSelectedPadding;
    }
}
