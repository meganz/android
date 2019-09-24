package mega.privacy.android.app.utils;

import java.util.ArrayList;

import nz.mega.sdk.MegaNode;

public class MegaNodeUtil {

    /*
     * The method to calculate how many nodes are folders in array list
     */
    public static int getNumberOfFolders(ArrayList<MegaNode> nodes) {
        int folderCount = 0;
        if (nodes == null) return folderCount;
        for (MegaNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            }
        }
        return folderCount;
    }
}
