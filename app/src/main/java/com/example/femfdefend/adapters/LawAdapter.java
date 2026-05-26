package com.example.femfdefend.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.femfdefend.LawDetailActivity;
import com.example.femfdefend.R;
import com.example.femfdefend.models.Law;
import com.example.femfdefend.utils.SearchUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LawAdapter extends RecyclerView.Adapter<LawAdapter.LawViewHolder> {
    private List<Law> laws;
    private List<Law> filteredLaws;
    private String currentQuery = "";

    public LawAdapter() {
        this.laws = new ArrayList<>();
        this.filteredLaws = new ArrayList<>();
    }

    @NonNull
    @Override
    public LawViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_law, parent, false);
        return new LawViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LawViewHolder holder, int position) {
        Law law = filteredLaws.get(position);
        holder.bind(law);
    }

    @Override
    public int getItemCount() {
        return filteredLaws.size();
    }

    public void updateLaws(List<Law> newLaws) {
        this.laws = new ArrayList<>(newLaws);
        filter(currentQuery); // Apply current filter
    }

    public boolean filter(String query) {
        currentQuery = query.toLowerCase().trim();
        filteredLaws.clear();

        if (currentQuery.isEmpty()) {
            // Show all laws when no query
            filteredLaws.addAll(laws);
            notifyDataSetChanged();
            return true;
        }

        // Calculate relevance scores and filter laws
        for (Law law : laws) {
            double score = SearchUtils.calculateRelevanceScore(currentQuery, law.getTitle());
            if (score > 0) {
                Law lawCopy = new Law.Builder()
                    .id(law.getId())
                    .title(law.getTitle())
                    .shortDescription(law.getShortDescription())
                    .fullDescription(law.getFullDescription())
                    .year(law.getYear())
                    .build();
                lawCopy.setRelevanceScore(score);
                filteredLaws.add(lawCopy);
            }
        }

        // Sort by relevance
        Collections.sort(filteredLaws);

        // If no exact matches but query has relevant keywords, add related laws
        if (filteredLaws.isEmpty()) {
            for (Law law : laws) {
                if (SearchUtils.isRelevant(currentQuery, law.getTitle())) {
                    Law lawCopy = new Law.Builder()
                        .id(law.getId())
                        .title(law.getTitle())
                        .shortDescription(law.getShortDescription())
                        .fullDescription(law.getFullDescription())
                        .year(law.getYear())
                        .build();
                    lawCopy.setRelevanceScore(0.5); // Lower relevance score for related laws
                    filteredLaws.add(lawCopy);
                }
            }
        }

        notifyDataSetChanged();
        return !filteredLaws.isEmpty();
    }

    static class LawViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvDescription;
        private final MaterialButton btnReadMore;

        public LawViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvYear = itemView.findViewById(R.id.tvYear);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnReadMore = itemView.findViewById(R.id.btnReadMore);
        }

        public void bind(Law law) {
            tvTitle.setText(law.getTitle());
            tvYear.setText(itemView.getContext().getString(R.string.year, law.getYear()));
            tvDescription.setText(law.getShortDescription());

            btnReadMore.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), LawDetailActivity.class);
                intent.putExtra("law_title", law.getTitle());
                intent.putExtra("law_description", law.getFullDescription());
                intent.putExtra("law_year", law.getYear());
                itemView.getContext().startActivity(intent);
            });

            // Expand/collapse functionality
            itemView.setOnClickListener(v -> {
                if (tvDescription.getText().toString().equals(law.getShortDescription())) {
                    tvDescription.setText(law.getFullDescription());
                } else {
                    tvDescription.setText(law.getShortDescription());
                }
            });
        }
    }
} 