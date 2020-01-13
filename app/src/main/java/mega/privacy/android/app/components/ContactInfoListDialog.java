package mega.privacy.android.app.components;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
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
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.InvitationContactInfo;

public class ContactInfoListDialog {

    private Context context;

    private LayoutInflater inflater;

    private View contentView;

    private RecyclerView listView;

    private AlertDialog dialog;

    private InvitationContactInfo current;

    private Set<InvitationContactInfo> selected = new HashSet<>();

    private static final float WIDTH_P = 0.9f;
    private static final float HEIGHT_P = 0.7f;
    private static final float WIDTH_L = 0.5f;
    private static final float HEIGHT_L = 0.9f;
    private static final float CHECKBOX_ALAPHA = 0.3f;

    public ContactInfoListDialog(@NonNull Context context, InvitationContactInfo contact, final OnMultipleSelectedListener listener) {
        this.context = context;
        this.current = contact;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.dialog_contact_info_list, null);
        listView = contentView.findViewById(R.id.info_list_view);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        contentView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        contentView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                listener.onSelect(selected);
            }
        });
    }

    /**
     * @param addedContacts The contact info which are already added to the EditText.
     */
    public void showInfo(ArrayList<InvitationContactInfo> addedContacts) {
        selected.clear();
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
            if (onLandscapeMode()) {
                window.setLayout((int) (screenW * WIDTH_L), (int) (screenH * HEIGHT_L));
            } else {
                window.setLayout((int) (screenW * WIDTH_P), (int) (screenH * HEIGHT_P));
            }
        }
    }

    private boolean onLandscapeMode() {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void recycle() {
        if(dialog != null) {
            dialog.dismiss();
        }
    }

    public interface OnMultipleSelectedListener {

        /**
         * @param contactInfos The selected/unselected contact infos.
         */
        void onSelect(Set<InvitationContactInfo> contactInfos);
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
            if (added.contains(content)) {
                viewHolder.checkBox.setChecked(true);
                viewHolder.checkBox.setAlpha(1.0f);
            } else {
                viewHolder.checkBox.setChecked(false);
                viewHolder.checkBox.setAlpha(CHECKBOX_ALAPHA);
            }
            // set listener after `setChecked`, or the listener will trigger
            viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String content = contents.get(viewHolder.getAdapterPosition());
                    InvitationContactInfo info = null;
                    try {
                        info = (InvitationContactInfo) current.clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    // In fact the `CloneNotSupportedException` would never happen, so `info` would never be `null`.
                    info.setDisplayInfo(content);
                    // ignore `isChecked` the callback will handle.
                    selected.add(info);
                    //UI update
                    if (isChecked) {
                        buttonView.setAlpha(1.0f);
                    } else {
                        buttonView.setAlpha(CHECKBOX_ALAPHA);
                    }
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
}
