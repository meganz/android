package mega.privacy.android.app.fragments.managerFragments.cu;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
public class CuItemSizeConfig {
    private final boolean mSmallGrid;
    private final int mGridSize;
    private final int mIcSelectedSize;
    private final int mSelectedPadding;
    private final int mIcSelectedMargin;

    public CuItemSizeConfig(boolean smallGrid, int gridSize, int icSelectedSize, int selectedPadding, int icSelectedMargin) {
        mSmallGrid = smallGrid;
        mGridSize = gridSize;
        mIcSelectedSize = icSelectedSize;
        mSelectedPadding = selectedPadding;
        mIcSelectedMargin = icSelectedMargin;
    }

    public boolean isSmallGrid() {
        return mSmallGrid;
    }

    public int getGridSize() {
        return mGridSize;
    }

    public int getIcSelectedSize() {
        return mIcSelectedSize;
    }

    public int getSelectedPadding() {
        return mSelectedPadding;
    }

    public int getIcSelectedMargin() {
        return mIcSelectedMargin;
    }
}
