package mega.privacy.android.app.components;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.InvitationContactInfo;

import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

public class ContactInfoListDialog {

    private Context context;

    private LayoutInflater inflater;

    private View contentView;

    private RecyclerView listView;

    private AlertDialog dialog;

    private InvitationContactInfo current;

    private Set<InvitationContactInfo> selected = new HashSet<>();

    private Set<InvitationContactInfo> unSelected = new HashSet<>();

    private ArrayList<Integer> checkedIndex = new ArrayList<>();

    private boolean isResumed;

    /**
     * proportion of screen width under portrait mode
     */
    private static final float WIDTH_P = 0.9f;
    /**
     * proportion of screen height under portrait mode
     */
    private static final float HEIGHT_P = 0.5f;
    /**
     * proportion of screen width under landscape mode
     */
    private static final float WIDTH_L = 0.5f;
    /**
     * proportion of screen height under portrait mode
     */
    private static final float HEIGHT_L = 0.9f;
    private static final float CHECKBOX_ALPHA = 0.3f;

    public ContactInfoListDialog(@NonNull Context context, InvitationContactInfo contact, final OnMultipleSelectedListener listener) {
        this.context = context;
        this.current = contact;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.dialog_contact_info_list, null);
        listView = contentView.findViewById(R.id.info_list_view);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        contentView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
            listener.cancel();

        });
        contentView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onSelect(selected, unSelected);
        });
    }

    /**
     * @param addedContacts The contact info which are already added to the EditText.
     */
    public void showInfo(ArrayList<InvitationContactInfo> addedContacts, boolean isResumed) {
        this.isResumed = isResumed;
        List<String> added = new ArrayList<>();
        if (addedContacts != null && addedContacts.size() != 0) {
            for (InvitationContactInfo info : addedContacts) {
                // only add to the same local contact, in case different contacts has same contact info.
                if (info.getId() == current.getId()) {
                    added.add(info.getDisplayInfo());
                }
            }
        }

        listView.setAdapter(new ContactInfoAdapter(current.getFilteredContactInfos(), added));
        dialog = new AlertDialog.Builder(context)
                .setTitle(current.getName())
                .setView(contentView)
                .setCancelable(false)
                .create();
        dialog.show();
        // get current device's screen size in pixel
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        setDialogSize(width, height);
    }

    private void setDialogSize(int screenW, int screenH) {
        Window window = dialog.getWindow();
        DisplayMetrics metrics = new DisplayMetrics();
        if (window != null) {
            Display display = window.getWindowManager().getDefaultDisplay();
            display.getMetrics(metrics);
            if (isScreenInPortrait(context)) {
                window.setLayout((int) (screenW * WIDTH_P), (int) (screenH * HEIGHT_P));
            } else {
                window.setLayout((int) (screenW * WIDTH_L), (int) (screenH * HEIGHT_L));
            }
        }
    }

    public void recycle() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public interface OnMultipleSelectedListener {

        /**
         * @param contactInfos The selected/unselected contact infos.
         */
        void onSelect(@NonNull Set<InvitationContactInfo> contactInfos, @NonNull Set<InvitationContactInfo> toRemove);

        void cancel();
    }

    private class ContactInfoAdapter extends RecyclerView.Adapter<ContactInfoAdapter.ContactInfoViewHolder> {

        /**
         * The filtered contact info list of a local contact.
         */
        private List<String> contents;

        /**
         * The already added contact info of the local contact.
         */
        private List<String> added;

        private ContactInfoAdapter(List<String> contents, List<String> added) {
            this.contents = contents;
            this.added = added;
        }

        @NonNull
        @Override
        public ContactInfoViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
            View itemView = inflater.inflate(R.layout.contact_info_item, viewGroup, false);
            ContactInfoViewHolder holder = new ContactInfoViewHolder(itemView);
            itemView.setTag(holder);
            return holder;
        }

        @Override
        public int getItemCount() {
            return contents.size();
        }

        @Override
        public void onBindViewHolder(@NonNull final ContactInfoAdapter.ContactInfoViewHolder viewHolder, int i) {
            String content = contents.get(i);
            viewHolder.textView.setText(content);
            viewHolder.textView.setOnClickListener(v ->
                    viewHolder.checkBox.setChecked(!viewHolder.checkBox.isChecked())
            );
            if (isResumed) {
                if (checkedIndex.contains(i)) {
                    viewHolder.checkBox.setChecked(true);
                    viewHolder.checkBox.setAlpha(1.0f);
                } else {
                    viewHolder.checkBox.setChecked(false);
                    viewHolder.checkBox.setAlpha(CHECKBOX_ALPHA);
                }
            } else {
                if (added.contains(content)) {
                    checkedIndex.add(i);
                    viewHolder.checkBox.setChecked(true);
                    viewHolder.checkBox.setAlpha(1.0f);
                } else {
                    viewHolder.checkBox.setChecked(false);
                    viewHolder.checkBox.setAlpha(CHECKBOX_ALPHA);
                }
            }

            // set listener after `setChecked`, or the listener will trigger
            viewHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = viewHolder.getAdapterPosition();
                InvitationContactInfo info = null;
                try {
                    info = (InvitationContactInfo) current.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                /*
                 * In fact the `CloneNotSupportedException` would never happen,
                 * as long as `InvitationContactInfo` implements `Cloneable`,
                 * so `info` would never be `null`.
                 */
                assert info != null;
                info.setDisplayInfo(content);

                if (isChecked) {
                    checkedIndex.add(position);
                    selected.add(info);
                    unSelected.remove(info);
                    buttonView.setAlpha(1.0f);
                } else {
                    checkedIndex.remove(Integer.valueOf(position));
                    selected.remove(info);
                    unSelected.add(info);
                    buttonView.setAlpha(CHECKBOX_ALPHA);
                }
            });
        }

        private class ContactInfoViewHolder extends RecyclerView.ViewHolder {

            private TextView textView;

            private CheckBox checkBox;

            private ContactInfoViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.checkbox);
                textView = itemView.findViewById(R.id.content);
            }
        }
    }

    public ArrayList<Integer> getCheckedIndex() {
        return checkedIndex;
    }

    public void setCheckedIndex(ArrayList<Integer> checkedIndex) {
        if (checkedIndex != null) {
            this.checkedIndex = checkedIndex;
        }
    }

    public Set<InvitationContactInfo> getSelected() {
        return selected;
    }

    public void setSelected(Set<InvitationContactInfo> selected) {
        if (selected != null) {
            this.selected = selected;
        }
    }

    public Set<InvitationContactInfo> getUnSelected() {
        return unSelected;
    }

    public void setUnSelected(Set<InvitationContactInfo> unSelected) {
        if (unSelected != null) {
            this.unSelected = unSelected;
        }
    }
}
