package com.martru118.fundict;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.Helper.ThemeHelper;
import com.paulrybitskyi.persistentsearchview.PersistentSearchView;
import com.paulrybitskyi.persistentsearchview.adapters.model.SuggestionItem;
import com.paulrybitskyi.persistentsearchview.listeners.OnSearchConfirmedListener;
import com.paulrybitskyi.persistentsearchview.listeners.OnSearchQueryChangeListener;
import com.paulrybitskyi.persistentsearchview.listeners.OnSuggestionChangeListener;
import com.paulrybitskyi.persistentsearchview.utils.SuggestionCreationUtil;

public class SearchActivity extends AppCompatActivity {
    private PersistentSearchView mSearchBar;
    private DatabaseOpenHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        db = new DatabaseOpenHelper(this);

        //set up searchview
        mSearchBar = findViewById(R.id.search_bar);
        initTheme();
        mSearchBar.setSuggestions(SuggestionCreationUtil.asRecentSearchSuggestions(db.getHistory(true)), false);
        mSearchBar.setOnSearchConfirmedListener(new OnSearchConfirmedListener() {
            @Override
            public void onSearchConfirmed(PersistentSearchView searchView, String query) {
                if (db.exists("words", query))
                    getResults(query);
                else
                    Toast.makeText(SearchActivity.this, "Word not found", Toast.LENGTH_SHORT).show();
            }
        });
        mSearchBar.setOnLeftBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mSearchBar.isExpanded()) {
                    setResult(RESULT_CANCELED, new Intent());
                    finish();
                }
            }
        });
        mSearchBar.setOnSearchQueryChangeListener(new OnSearchQueryChangeListener() {
            @Override
            public void onSearchQueryChanged(PersistentSearchView searchView, String oldQuery, String newQuery) {
                if (newQuery!=null && newQuery.length()>0)
                    mSearchBar.setSuggestions(SuggestionCreationUtil.asRegularSearchSuggestions(db.getSuggestions(newQuery)));
                else
                    mSearchBar.setSuggestions(SuggestionCreationUtil.asRecentSearchSuggestions(db.getHistory(true)));
            }
        });
        mSearchBar.setOnSuggestionChangeListener(new OnSuggestionChangeListener() {
            @Override
            public void onSuggestionPicked(SuggestionItem suggestion) {
                getResults(suggestion.getItemModel().getText());
            }

            @Override
            public void onSuggestionRemoved(SuggestionItem suggestion) {
                //update search history
                db.removefromHistory(suggestion.getItemModel().getText());
                mSearchBar.setSuggestions(SuggestionCreationUtil.asRecentSearchSuggestions(db.getHistory(true)));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mSearchBar.isExpanded())
            mSearchBar.collapse();
        else
            super.onBackPressed();
    }

    private void getResults(String query) {
        Intent doSearch = new Intent();
        doSearch.putExtra("result", query);

        db.addtoHistory(query, 1);
        setResult(RESULT_OK, doSearch);
        finish();
    }

    private void initTheme() {
        ThemeHelper theme = new ThemeHelper(this);
        if (theme.loadNightMode())
            mSearchBar.setCardBackgroundColor(getResources().getColor(R.color.searchBarPrimaryColorDark));
        else
            mSearchBar.setCardBackgroundColor(getResources().getColor(R.color.white));
    }
}
