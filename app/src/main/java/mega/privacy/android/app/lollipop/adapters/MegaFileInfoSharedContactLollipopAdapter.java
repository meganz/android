package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

public class MegaFileInfoSharedContactLollipopAdapter extends MegaSharedFolderLollipopAdapter {

    public MegaFileInfoSharedContactLollipopAdapter(Context _context,MegaNode node,ArrayList<MegaShare> _shareList,RecyclerView _lv) {
        super(_context,node,_shareList,_lv);
    }
    
    @Override
    public void onClick(View v) {
        LogUtil.logDebug("onClick");
        
        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;
        final MegaShare s = (MegaShare) getItem(currentPosition);
        
        switch (v.getId()){
            case R.id.shared_folder_three_dots_layout:{
                if(multipleSelect){
                    ((FileInfoActivityLollipop) context).itemClick(currentPosition);
                }
                else{
                    ((FileInfoActivityLollipop) context).showOptionsPanel(s);
                }
                
                break;
            }
            case R.id.shared_folder_item_layout:{
                ((FileInfoActivityLollipop) context).itemClick(currentPosition);
                break;
            }
        }
    }
    
    public void toggleSelection(int pos) {
        LogUtil.logDebug("Position: " + pos);
        if (selectedItems.get(pos, false)) {
            LogUtil.logDebug("Delete pos: " + pos);
            selectedItems.delete(pos);
        }
        else {
            LogUtil.logDebug("PUT pos: " + pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
        
        MegaSharedFolderLollipopAdapter.ViewHolderShareList view = (MegaSharedFolderLollipopAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
        if(view!=null){
            LogUtil.logDebug("Start animation: " + pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    
                }
                
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0){
                        ((FileInfoActivityLollipop) context).hideMultipleSelect();
                    }
                }
                
                @Override
                public void onAnimationRepeat(Animation animation) {
                    
                }
            });
            view.imageView.startAnimation(flipAnimation);
        }
    }
    
    public void toggleAllSelection(int pos) {
        LogUtil.logDebug("Position: " + pos);
        final int positionToflip = pos;
        
        if (selectedItems.get(pos, false)) {
            LogUtil.logDebug("Delete pos: " + pos);
            selectedItems.delete(pos);
        }
        else {
            LogUtil.logDebug("PUT pos: " + pos);
            selectedItems.put(pos, true);
        }

        LogUtil.logDebug("Adapter type is LIST");
        MegaSharedFolderLollipopAdapter.ViewHolderShareList view = (MegaSharedFolderLollipopAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
        if(view!=null){
            LogUtil.logDebug("Start animation: " + pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    
                }
                
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0){
                        ((FileInfoActivityLollipop) context).hideMultipleSelect();
                    }
                    notifyItemChanged(positionToflip);
                }
                
                @Override
                public void onAnimationRepeat(Animation animation) {
                    
                }
            });
            view.imageView.startAnimation(flipAnimation);
        }
        else{
            LogUtil.logError("NULL view pos: " + positionToflip);
            notifyItemChanged(pos);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;

        ((FileInfoActivityLollipop) context).activateActionMode();
        ((FileInfoActivityLollipop) context).itemClick(currentPosition);

        return true;
    }
}
