package mega.privacy.android.app.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.InvitationContactInfo;

public class ContactInfoListDialog {

    private Context context;

    private LayoutInflater inflater;

    private View contentView;

    private RecyclerView listView;

    private AlertDialog dialog;

    private InvitationContactInfo current;

    private List<InvitationContactInfo> selected = new ArrayList<>();

    public ContactInfoListDialog(@NonNull Context context, InvitationContactInfo contact, final OnMultipleSelectedListener listener) {
        this.context = context;
        this.current = contact;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.dialog_contact_info_list, null);
        listView = contentView.findViewById(R.id.info_list_view);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        contentView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                listener.onSelect(selected);
            }
        });
    }

    public void showInfo(ArrayList<InvitationContactInfo> addedContacts) {
        selected.clear();
        List<String> added = new ArrayList<>();
        if (addedContacts != null && addedContacts.size() != 0) {
            for (InvitationContactInfo info : addedContacts) {
                added.add(info.getDisplayInfo());
            }
        }

        dialog = new AlertDialog.Builder(context)
                .setTitle(current .getName() )
                .setView(contentView)
                .create();
        listView.setAdapter(new ContactInfoAdapter(current.getFilteredContactInfos(), added));
        dialog.show();
    }

    public interface OnMultipleSelectedListener {

        void onSelect(List<InvitationContactInfo> selected);
    }

    private class ContactInfoAdapter extends RecyclerView.Adapter<ContactInfoAdapter.ContactInfoViewHolder> {

        private List<String> contents;

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
                // content will be added to `selected`.
                viewHolder.checkBox.setChecked(true);
            } else {
                viewHolder.checkBox.setChecked(false);
            }
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
