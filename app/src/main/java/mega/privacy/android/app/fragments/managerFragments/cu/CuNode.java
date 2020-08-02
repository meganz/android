package mega.privacy.android.app.fragments.managerFragments.cu;

import java.io.File;
import nz.mega.sdk.MegaNode;

public class CuNode {
    public static final int TYPE_TITLE = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_VIDEO = 3;

    private final MegaNode node;
    private final int index;
    private final File thumbnail;
    private final int type;
    private final String modifyDate;

    private boolean selected;

    public CuNode(MegaNode node, int index, File thumbnail, int type, String modifyDate,
            boolean selected) {
        this.node = node;
        this.index = index;
        this.thumbnail = thumbnail;
        this.type = type;
        this.modifyDate = modifyDate;
        this.selected = selected;
    }

    public MegaNode getNode() {
        return node;
    }

    public int getIndex() {
        return index;
    }

    public File getThumbnail() {
        return thumbnail;
    }

    public int getType() {
        return type;
    }

    public String getModifyDate() {
        return modifyDate;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
