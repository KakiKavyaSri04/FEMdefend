package com.example.femfdefend.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.femfdefend.R;
import com.example.femfdefend.models.Helpline;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class HelplineAdapter extends RecyclerView.Adapter<HelplineAdapter.HelplineViewHolder> {
    private List<Helpline> helplines;

    public HelplineAdapter(List<Helpline> helplines) {
        this.helplines = helplines;
    }

    @NonNull
    @Override
    public HelplineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_helpline, parent, false);
        return new HelplineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HelplineViewHolder holder, int position) {
        Helpline helpline = helplines.get(position);
        holder.bind(helpline);
    }

    @Override
    public int getItemCount() {
        return helplines != null ? helplines.size() : 0;
    }

    public void setHelplines(List<Helpline> helplines) {
        this.helplines = helplines;
        notifyDataSetChanged();
    }

    static class HelplineViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvNumber;
        private final TextView tvDescription;
        private final MaterialButton btnCall;

        public HelplineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnCall = itemView.findViewById(R.id.btnCall);
        }

        public void bind(Helpline helpline) {
            tvName.setText(helpline.getName());
            tvNumber.setText(helpline.getNumber());
            tvDescription.setText(helpline.getDescription());

            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + helpline.getNumber()));
                v.getContext().startActivity(intent);
            });
        }
    }
} 