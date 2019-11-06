package mega.privacy.android.app.utils;

import java.util.ArrayList;
import java.util.Collections;

import mega.privacy.android.app.MegaOffline;
import nz.mega.sdk.MegaNode;

public class SortUtil {

    public static void sortByMailDescending(ArrayList<MegaNode> nodes){
        ArrayList<MegaNode> folderNodes = new ArrayList<>();
        ArrayList<MegaNode> fileNodes = new ArrayList<>();

        for (int i=0;i<nodes.size();i++){
            if(nodes.get(i) == null) {
                continue;
            }
            if (nodes.get(i).isFolder()){
                folderNodes.add(nodes.get(i));
            }
            else{
                fileNodes.add(nodes.get(i));
            }
        }

        Collections.reverse(folderNodes);
        Collections.reverse(fileNodes);

        nodes.clear();
        nodes.addAll(folderNodes);
        nodes.addAll(fileNodes);
    }

    public static void sortByNameDescending(ArrayList<MegaNode> nodes){
        ArrayList<String> foldersOrder = new ArrayList<>();
        ArrayList<String> filesOrder = new ArrayList<>();
        ArrayList<MegaNode> tempOffline = new ArrayList<>();


        for(int k = 0; k < nodes.size() ; k++) {
            MegaNode node = nodes.get(k);
            if(node == null) {
                continue;
            }
            if(node.isFolder()){
                foldersOrder.add(node.getName());
            }
            else{
                filesOrder.add(node.getName());
            }
        }


        Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.reverse(foldersOrder);
        Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.reverse(filesOrder);

        for(int k = 0; k < foldersOrder.size() ; k++) {
            for(int j = 0; j < nodes.size() ; j++) {
                String name = foldersOrder.get(k);
                if(nodes.get(j) == null) {
                    continue;
                }
                String nameOffline = nodes.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(nodes.get(j));
                }
            }

        }

        for(int k = 0; k < filesOrder.size() ; k++) {
            for(int j = 0; j < nodes.size() ; j++) {
                String name = filesOrder.get(k);
                if(nodes.get(j) == null) {
                    continue;
                }
                String nameOffline = nodes.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(nodes.get(j));
                }
            }

        }

        nodes.clear();
        nodes.addAll(tempOffline);
    }

    public static void sortByNameAscending(ArrayList<MegaNode> nodes){
        ArrayList<String> foldersOrder = new ArrayList<>();
        ArrayList<String> filesOrder = new ArrayList<>();
        ArrayList<MegaNode> tempOffline = new ArrayList<>();

        for(int k = 0; k < nodes.size() ; k++) {
            MegaNode node = nodes.get(k);
            if(node == null) {
                continue;
            }
            if(node.isFolder()){
                foldersOrder.add(node.getName());
            }
            else{
                filesOrder.add(node.getName());
            }
        }

        Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

        for(int k = 0; k < foldersOrder.size() ; k++) {
            for(int j = 0; j < nodes.size() ; j++) {
                String name = foldersOrder.get(k);
                if(nodes.get(j) == null) {
                    continue;
                }
                String nameOffline = nodes.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(nodes.get(j));
                }
            }
        }

        for(int k = 0; k < filesOrder.size() ; k++) {
            for(int j = 0; j < nodes.size() ; j++) {
                String name = filesOrder.get(k);
                if(nodes.get(j) == null) {
                    continue;
                }
                String nameOffline = nodes.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(nodes.get(j));
                }
            }

        }

        nodes.clear();
        nodes.addAll(tempOffline);
    }

    public static void sortOfflineByNameDescending(ArrayList<MegaOffline> mOffList) {
        sortOffline(mOffList, true);
    }


    public static void sortOfflineByNameAscending(ArrayList<MegaOffline> mOffList) {
        sortOffline(mOffList, false);
    }

    private static void sortOffline(ArrayList<MegaOffline> mOffList, boolean reverse) {
        if (mOffList.size() <= 0) {
            return;
        }

        ArrayList<String> foldersOrder = new ArrayList<>();
        ArrayList<String> filesOrder = new ArrayList<>();
        ArrayList<MegaOffline> tempOffline = new ArrayList<>();

        for (int k = 0; k < mOffList.size(); k++) {
            MegaOffline node = mOffList.get(k);
            if (node == null) {
                continue;
            }
            if (node.getType().equals("1")) {
                foldersOrder.add(node.getName());
            } else {
                filesOrder.add(node.getName());
            }
        }

        Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
        if (reverse) {
            Collections.reverse(foldersOrder);
            Collections.reverse(filesOrder);
        }

        for (int k = 0; k < foldersOrder.size(); k++) {
            for (int j = 0; j < mOffList.size(); j++) {
                String name = foldersOrder.get(k);
                MegaOffline offline = mOffList.get(j);
                if (offline == null) {
                    continue;
                }
                String nameOffline = offline.getName();
                if (name.equals(nameOffline)) {
                    tempOffline.add(offline);
                }
            }
        }

        for (int k = 0; k < filesOrder.size(); k++) {
            for (int j = 0; j < mOffList.size(); j++) {
                String name = filesOrder.get(k);
                MegaOffline offline = mOffList.get(j);
                if (offline == null) {
                    continue;
                }
                String nameOffline = offline.getName();
                if (name.equals(nameOffline)) {
                    tempOffline.add(offline);
                }
            }
        }

        mOffList.clear();
        mOffList.addAll(tempOffline);
    }
}
