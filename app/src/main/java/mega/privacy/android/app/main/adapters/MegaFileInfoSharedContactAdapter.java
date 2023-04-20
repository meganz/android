package mega.privacy.android.app.main.adapters;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import timber.log.Timber;

public class MegaFileInfoSharedContactAdapter extends MegaSharedFolderAdapter {

    public interface MegaFileInfoSharedContactAdapterListener {

        void itemClick(int currentPosition);

        void showOptionsPanel(MegaShare share);

        void hideMultipleSelect();

        void activateActionMode();
    }

    public MegaFileInfoSharedContactAdapter(Context _context, MegaNode node, List<MegaShare> _shareList, RecyclerView _lv, MegaFileInfoSharedContactAdapterListener listener) {
        super(_context, node, _shareList, _lv);
        this.listener = listener;
    }
    
    private MegaFileInfoSharedContactAdapterListener listener;

    @Override
    public void onClick(View v) {
        Timber.d("onClick");

        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;
        final MegaShare s = (MegaShare) getItem(currentPosition);

        int id = v.getId();
        if (id == R.id.shared_folder_three_dots_layout) {
            if (multipleSelect) {
                listener.itemClick(currentPosition);
            } else {
                listener.showOptionsPanel(s);
            }
        } else if (id == R.id.shared_folder_item_layout) {
            listener.itemClick(currentPosition);
        }
    }

    public void toggleSelection(int pos) {
        Timber.d("Position: %s", pos);
        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);

        MegaSharedFolderAdapter.ViewHolderShareList view = (MegaSharedFolderAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        listener.hideMultipleSelect();
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
        Timber.d("Position: %s", pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        Timber.d("Adapter type is LIST");
        MegaSharedFolderAdapter.ViewHolderShareList view = (MegaSharedFolderAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        listener.hideMultipleSelect();
                    }
                    notifyItemChanged(positionToflip);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            Timber.e("NULL view pos: %s", positionToflip);
            notifyItemChanged(pos);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;

        listener.activateActionMode();
        listener.itemClick(currentPosition);

        return true;
    }
}
