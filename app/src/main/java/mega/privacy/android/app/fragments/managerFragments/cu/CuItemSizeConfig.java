package mega.privacy.android.app.fragments.managerFragments.cu;

public class CuItemSizeConfig {
    private final boolean mSmallGrid;
    private final int mGridSize;
    private final int mIcSelectedSize;
    private final int mImageMargin;
    private final int mImageSelectedPadding;
    private final int mIcSelectedMargin;
    private final int mRoundCornerRadius;

    public CuItemSizeConfig(boolean smallGrid, int gridSize, int icSelectedSize, int imageMargin,
            int imageSelectedPadding, int icSelectedMargin, int roundCornerRadius) {
        mSmallGrid = smallGrid;
        mGridSize = gridSize;
        mIcSelectedSize = icSelectedSize;
        mImageMargin = imageMargin;
        mImageSelectedPadding = imageSelectedPadding;
        mIcSelectedMargin = icSelectedMargin;
        mRoundCornerRadius = roundCornerRadius;
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

    public int getImageMargin() {
        return mImageMargin;
    }

    public int getImageSelectedPadding() {
        return mImageSelectedPadding;
    }

    public int getIcSelectedMargin() {
        return mIcSelectedMargin;
    }

    public int getRoundCornerRadius() {
        return mRoundCornerRadius;
    }
}
