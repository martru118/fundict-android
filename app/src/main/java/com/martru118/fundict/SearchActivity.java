package com.martru118.fundict;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private final String pattern = "^[a-zA-Z0-9 ]*$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        db = new DatabaseOpenHelper(this);

        //set up searchview
        mSearchBar = findViewById(R.id.search_bar);
        initTheme();

        mSearchBar.setOnLeftBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //exit activity
                if (!mSearchBar.isExpanded()) {
                    setResult(RESULT_CANCELED, new Intent());
                    finish();
                }
            }
        });

        mSearchBar.setOnSearchConfirmedListener(new OnSearchConfirmedListener() {
            @Override
            public void onSearchConfirmed(PersistentSearchView searchView, String query) {
                String confirmQuery = query.toLowerCase();

                if (query.matches(pattern)) {
                    if (db.exists("words", confirmQuery))
                        getSearchResults(confirmQuery);
                    else
                        Toast.makeText(SearchActivity.this, "Word not found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SearchActivity.this, "Word is invalid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mSearchBar.setOnSearchQueryChangeListener(new OnSearchQueryChangeListener() {
            @Override
            public void onSearchQueryChanged(PersistentSearchView searchView, String oldQuery, String newQuery) {
                if (newQuery!=null && newQuery.length()>0 && newQuery.matches(pattern))
                    mSearchBar.setSuggestions(SuggestionCreationUtil.asRegularSearchSuggestions(db.getSuggestions(newQuery)));
                else
                    mSearchBar.setSuggestions(SuggestionCreationUtil.asRecentSearchSuggestions(db.getHistory(true)));
            }
        });

        mSearchBar.setOnSuggestionChangeListener(new OnSuggestionChangeListener() {
            @Override
            public void onSuggestionPicked(SuggestionItem suggestion) {
                getSearchResults(suggestion.getItemModel().getText());
            }

            @Override
            public void onSuggestionRemoved(SuggestionItem suggestion) {
                //update search history
                db.removefromHistory(suggestion.getItemModel().getText(), false);
                mSearchBar.setSuggestions(SuggestionCreationUtil.asRecentSearchSuggestions(db.getHistory(true)));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mSearchBar.isExpanded())
            mSearchBar.collapse();  //exit search
        else
            super.onBackPressed();  //exit activity
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSearchBar.setSuggestions(SuggestionCreationUtil.asRecentSearchSuggestions(db.getHistory(true)), false);
    }

    /**
     * Sends search query to MainActivity where its definition would be retrieved and displayed.
     *
     * @param query -- The search query to be sent to the MainActivity.
     */
    private void getSearchResults(String query) {
        Intent doSearch = new Intent();
        doSearch.putExtra("result", query);
        setResult(RESULT_OK, doSearch);
        finish();
    }

    /**
     * Set search bar color depending on app theme.
     */
    private void initTheme() {
        ThemeHelper theme = new ThemeHelper(this);
        if (theme.loadNightMode()) {
            //set night mode colors
            mSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.searchBarPrimaryColorDark));
        } else {
            //set day mode colors
            mSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        }
    }
}
