package com.example.femfdefend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.femfdefend.models.EmergencyContact;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {
    private List<EmergencyContact> contacts = new ArrayList<>();
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onCallClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact);
    }

    public EmergencyContactAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.bind(contact, listener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateContacts(List<EmergencyContact> newContacts) {
        this.contacts = new ArrayList<>(newContacts);
        notifyDataSetChanged();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView phoneText;

        ContactViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(android.R.id.text1);
            phoneText = itemView.findViewById(android.R.id.text2);
        }

        void bind(EmergencyContact contact, OnContactClickListener listener) {
            nameText.setText(contact.getName());
            phoneText.setText(contact.getPhone());
            
            itemView.setOnClickListener(v -> listener.onCallClick(contact));
            itemView.setOnLongClickListener(v -> {
                listener.onDeleteClick(contact);
                return true;
            });
        }
    }
} 