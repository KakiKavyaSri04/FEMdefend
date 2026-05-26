package com.example.femfdefend;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.femfdefend.adapters.HelplineAdapter;
import com.example.femfdefend.adapters.LawAdapter;
import com.example.femfdefend.databinding.ActivityWomenLawsBinding;
import com.example.femfdefend.models.Helpline;
import com.example.femfdefend.models.Law;

import java.util.ArrayList;
import java.util.List;

public class WomenLawsActivity extends AppCompatActivity {
    private ActivityWomenLawsBinding binding;
    private HelplineAdapter helplineAdapter;
    private LawAdapter lawAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWomenLawsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.women_laws);
        }

        setupRecyclerViews();
        setupSearchBar();
        loadHelplines();
        loadLaws();
    }

    private void setupRecyclerViews() {
        // Setup Helplines RecyclerView
        binding.recyclerHelplines.setLayoutManager(new LinearLayoutManager(this));
        helplineAdapter = new HelplineAdapter(new ArrayList<>());
        binding.recyclerHelplines.setAdapter(helplineAdapter);

        // Setup Laws RecyclerView
        binding.recyclerLaws.setLayoutManager(new LinearLayoutManager(this));
        lawAdapter = new LawAdapter();
        binding.recyclerLaws.setAdapter(lawAdapter);
    }

    private void setupSearchBar() {
        // Handle search button click
        binding.searchButton.setOnClickListener(v -> performSearch());

        // Handle keyboard search action
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // Handle real-time search (optional, can be removed if you want search only on submit)
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error message when user starts typing
                binding.searchErrorText.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch() {
        String query = binding.searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            // Show all laws when search is empty
            lawAdapter.filter("");
            binding.searchErrorText.setVisibility(View.GONE);
        } else {
            // Perform search and show error if no results
            boolean hasResults = lawAdapter.filter(query);
            binding.searchErrorText.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            binding.searchErrorText.setText(R.string.no_laws_found);
        }
    }

    private void loadHelplines() {
        List<Helpline> helplines = new ArrayList<>();
        helplines.add(new Helpline("Women's Helpline", "1091", "24/7 Women's Helpline"));
        helplines.add(new Helpline("Domestic Abuse", "181", "Women in Distress"));
        helplines.add(new Helpline("Police", "100", "Emergency Police Service"));
        helplineAdapter.setHelplines(helplines);
    }

    private void loadLaws() {
        List<Law> laws = new ArrayList<>();
        
        // 1. Protection of Women from Domestic Violence Act
        laws.add(new Law.Builder()
            .id("dvact2005")
            .title("The Protection of Women from Domestic Violence Act, 2005")
            .shortDescription("Protects women from domestic violence and provides legal remedies.")
            .fullDescription("The Protection of Women from Domestic Violence Act, 2005 is one of the key Acts and laws for women in India meant for protecting women who are oppressed by domestic violence. The law provides for strict legal actions against husbands that harass, abuse and maltreat women in their own houses. The law strives to provide protection orders, residence orders, as well as monetary relief, which is meant to secure their safety and general wellbeing.")
            .year(2005)
            .build());

        // 2. Sexual Harassment at Workplace Act
        laws.add(new Law.Builder()
            .id("posh2013")
            .title("The Sexual Harassment of Women at Workplace (Prevention, Prohibition, and Redressal) Act, 2013")
            .shortDescription("Deals with sexual harassment at workplace, mandates internal committees and POSH Policy for addressing complaints.")
            .fullDescription("It deals with sexual harassment of women at the work place. It provides a legal basis that will help curb any such harassments and protects women, hence creating a safe and fair working atmosphere free from sexual harassment of women and their abuse. The Act mandates establishing of internal committees and POSH Policy for addressing complaints and prompt redressing of the cases.")
            .year(2013)
            .build());

        // 3. Equal Remuneration Act
        laws.add(new Law.Builder()
            .id("era1976")
            .title("The Equal Remuneration Act, 1976")
            .shortDescription("Ensures equal pay for equal work regardless of gender.")
            .fullDescription("The Equal Remuneration Act, 1976 (ERA) provides for a gender non-discriminatory and equality based legislation that mandates that men and women both should be paid equal remuneration for the same or equal work that they do, thus providing them with equal benefits and economic opportunities at work.")
            .year(1976)
            .build());

        // 4. National Commission for Women Act
        laws.add(new Law.Builder()
            .id("ncw1990")
            .title("The National Commission for Women Act, 1990")
            .shortDescription("Established to safeguard and advance women's rights in India.")
            .fullDescription("The National Commission for Women (NCW) was formed in 1990 through the enactment of the NCW Act. Its primary objective is to safeguard and advance women's rights in India. The NCW actively tackles matters concerning gender-based discrimination, violence against women and various violations of women's rights.")
            .year(1990)
            .build());

        // 5. Indecent Representation of Women Act
        laws.add(new Law.Builder()
            .id("irw1986")
            .title("The Indecent Representation of Women (Prohibition) Act, 1986")
            .shortDescription("Prohibits inappropriate portrayal of women in media and advertisements.")
            .fullDescription("The law prohibits any inappropriate portrayal of women in print, media, or advertisements. It aims to safeguard women's dignity and decency, and encourages respect towards them. Publications like Universal and LexisNexis provide detailed information about The Indecent Representation of Women (Prohibition) Act, 1986, including its rules and regulations.")
            .year(1986)
            .build());

        lawAdapter.updateLaws(laws);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 