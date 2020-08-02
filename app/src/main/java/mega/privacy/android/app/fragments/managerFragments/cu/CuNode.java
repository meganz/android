package mega.privacy.android.app.fragments.managerFragments.cu;

import java.io.File;
import nz.mega.sdk.MegaNode;

public class CuNode {
    public static final int TYPE_TITLE = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_VIDEO = 3;

    private final MegaNode mNode;
    private final int mIndex;
    private final File mThumbnail;
    private final int mType;
    private final String mModifyDate;

    private boolean mSelected;

    public CuNode(MegaNode node, int index, File thumbnail, int type, String modifyDate,
            boolean selected) {
        mNode = node;
        mIndex = index;
        mThumbnail = thumbnail;
        mType = type;
        mModifyDate = modifyDate;
        mSelected = selected;
    }

    public MegaNode getNode() {
        return mNode;
    }

    public int getIndex() {
        return mIndex;
    }

    public File getThumbnail() {
        return mThumbnail;
    }

    public int getType() {
        return mType;
    }

    public String getModifyDate() {
        return mModifyDate;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
