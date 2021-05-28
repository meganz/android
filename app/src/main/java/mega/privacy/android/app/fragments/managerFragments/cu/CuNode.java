package mega.privacy.android.app.fragments.managerFragments.cu;

import android.util.Pair;

import java.io.File;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;

public class CuNode {
    public static final int TYPE_HEADER = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_VIDEO = 3;

    private final MegaNode mNode;
    private final int mIndexForViewer;
    private final File mThumbnail;
    private final File mPreview;
    private final int mType;
    private final String mModifyDate;
    private final Pair<String, String> mHeaderDate;

    private boolean mSelected;

    public CuNode(MegaNode node, int indexForViewer, File thumbnail, File preview, int type,
                  String modifyDate, boolean selected) {
        mNode = node;
        mIndexForViewer = indexForViewer;
        mThumbnail = thumbnail;
        mPreview = preview;
        mType = type;
        mModifyDate = modifyDate;
        mSelected = selected;
        mHeaderDate = null;
    }

    public CuNode(String modifyDate, Pair<String, String> headerDate) {
        mNode = null;
        mIndexForViewer = INVALID_VALUE;
        mThumbnail = null;
        mPreview = null;
        mType = TYPE_HEADER;
        mModifyDate = modifyDate;
        mSelected = false;
        mHeaderDate = headerDate;
    }

    public MegaNode getNode() {
        return mNode;
    }

    public int getIndexForViewer() {
        return mIndexForViewer;
    }

    public File getThumbnail() {
        return mThumbnail;
    }

    public File getPreview() {
        return mPreview;
    }

    public int getType() {
        return mType;
    }

    public String getModifyDate() {
        return mModifyDate;
    }

    public Pair<String, String> getHeaderDate() {
        return mHeaderDate;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
