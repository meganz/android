package mega.privacy.android.app.fragments.managerFragments.cu;

import java.io.File;
import nz.mega.sdk.MegaNode;

public class CuNode {
    public static final int TYPE_TITLE = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_VIDEO = 3;

    private final MegaNode mNode;
    private final int mIndexInParent;
    private final File mThumbnail;
    private final int mType;
    private final String mModifyDate;

    private boolean mSelected;

    public CuNode(MegaNode node, int indexInParent, File thumbnail, int type, String modifyDate,
            boolean selected) {
        mNode = node;
        mIndexInParent = indexInParent;
        mThumbnail = thumbnail;
        mType = type;
        mModifyDate = modifyDate;
        mSelected = selected;
    }

    public MegaNode getNode() {
        return mNode;
    }

    public int getIndexInParent() {
        return mIndexInParent;
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
