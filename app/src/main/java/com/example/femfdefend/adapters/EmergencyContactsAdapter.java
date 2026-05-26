package com.example.femfdefend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.femfdefend.R;
import com.example.femfdefend.models.EmergencyContact;

import java.util.List;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder> {
    private final List<EmergencyContact> contacts;
    private final List<EmergencyContact> selectedContacts;

    public EmergencyContactsAdapter(List<EmergencyContact> contacts, List<EmergencyContact> selectedContacts) {
        this.contacts = contacts;
        this.selectedContacts = selectedContacts;
        // Clear any previous selections
        this.selectedContacts.clear();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.nameTextView.setText(contact.getName());
        holder.phoneTextView.setText(contact.getPhone());
        
        // Set the checkbox state without triggering the listener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedContacts.contains(contact));
        
        // Set up the checkbox listener
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedContacts.contains(contact)) {
                    selectedContacts.add(contact);
                }
            } else {
                selectedContacts.remove(contact);
            }
        });

        // Make the whole item clickable
        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public List<EmergencyContact> getSelectedContacts() {
        return selectedContacts;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final TextView phoneTextView;
        final CheckBox checkBox;

        ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.nameTextView);
            phoneTextView = view.findViewById(R.id.phoneTextView);
            checkBox = view.findViewById(R.id.checkBox);
        }
    }
}
