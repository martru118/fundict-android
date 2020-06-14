package com.martru118.fundict.ui.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;

import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.MainActivity;
import com.martru118.fundict.Model.Definition;
import com.martru118.fundict.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class displays the definition in a fragment.
 * Additional options are also displayed in the fragment.
 * These options include: view previous definition, add/remove definition to/from favorites,
 * read the current word aloud, and copy the definition to clipboard.
 */
public class DefinitionFragment extends Fragment {
    private TextToSpeech pronunciation;
    private TextView word, type, defn;
    private BottomAppBar buttonsPanel;
    private ScrollView scrollView;

    private DatabaseOpenHelper db;

    private final String defaultKey = "welcome";
    private List<String> previous = new ArrayList<>();
    private int i_back;
    private boolean isFavorite;

    public DefinitionFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseOpenHelper(getContext());
        updateHistory();
        i_back = 0;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frame_definition, container, false);
        word = v.findViewById(R.id.word);
        type = v.findViewById(R.id.type);
        defn = v.findViewById(R.id.definition);
        scrollView = v.findViewById(R.id.scrollable);

        //set app bar
        buttonsPanel = v.findViewById(R.id.bottomAppBar);
        setupButtonsPanel();

        //set floating action button
        FloatingActionButton fab = v.findViewById(R.id.fab);
        TooltipCompat.setTooltipText(fab, "Random Definition");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Definition wordDef = db.findDefinition(null, true);
                show(wordDef, 0);
            }
        });

        setDefinition(db.findDefinition(defaultKey, false));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkFavorites();

        //start tts service
        if (pronunciation==null) {
            pronunciation = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR)
                        pronunciation.setLanguage(Locale.ENGLISH);
                    else
                        ((MainActivity)getActivity()).makeToast("Cannot load Text-to-Speech service");
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        //shutdown tts service
        if (pronunciation != null) {
            pronunciation.stop();
            pronunciation.shutdown();
            pronunciation = null;
        }
    }

    /**
     * Sets up the menu on the bottom app bar.
     */
    private void setupButtonsPanel() {
        buttonsPanel.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    //back button
                    case R.id.back:
                        if (i_back < previous.size()) {
                            //get previous word at index
                            Definition prev = db.findDefinition(previous.get(i_back), false);
                            setDefinition(prev);

                            //get the next previous word
                            i_back++;
                        } else {
                            ((MainActivity)getActivity()).makeToast("End of history");
                        }
                        return true;

                    //star checkbox
                    case R.id.fav_action:
                        Definition wordDef = getDefinition();

                        if (isFavorite) {
                            //remove word from favorites
                            db.removefromFavorites("word", wordDef.getWord());
                            item.setIcon(R.drawable.ic_star_border_white_24dp);
                            isFavorite = false;

                            ((MainActivity)getActivity()).makeToast("Removed from favorites");
                            return true;
                        } else {
                            //add words to favorites
                            db.addtoFavorites(wordDef);
                            item.setIcon(R.drawable.ic_star_24dp);
                            isFavorite = true;

                            ((MainActivity)getActivity()).makeToast("Added to favorites");
                            return true;
                        }

                    //text-to-speech
                    case R.id.tts:
                        CharSequence toSpeak = word.getText();
                        pronunciation.setSpeechRate(0.65f);
                        pronunciation.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                        return true;

                    //copy to clipboard
                    case R.id.copy:
                        Definition copyDef = getDefinition();
                        ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("definition", String.format("%s (%s) — %s", copyDef.getWord(), copyDef.getType(), copyDef.getDefn()));
                        clipboard.setPrimaryClip(clip);

                        ((MainActivity)getActivity()).makeToast("Definition copied to clipboard");
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    /**
     * Update history by retrieving all previous words.
     * Set the next previous word as index 1.
     */
    private void updateHistory() {
        previous = db.getHistory(false);
        i_back = 1;
    }

    /**
     * Checks if a word is in favorites and changes the star checkbox accordingly.
     */
    private void checkFavorites() {
        if (db.exists("favorites", getDefinition().getWord())) {
            //word is in favorites
            buttonsPanel.getMenu().getItem(1).setIcon(R.drawable.ic_star_24dp);
            isFavorite = true;
        } else {
            //word is not in favorites
            buttonsPanel.getMenu().getItem(1).setIcon(R.drawable.ic_star_border_white_24dp);
            isFavorite = false;
        }
    }

    /**
     * Retrieves the current definition.
     * Current definition is set from retrieving the text from each textview.
     *
     * @return The definition that is currently displayed.
     */
    private Definition getDefinition() {
        Definition wordDef = new Definition();
        wordDef.setWord(word.getText().toString());
        wordDef.setType(type.getText().toString().replace("\n", "/"));
        wordDef.setDefn(defn.getText().toString().substring(0, defn.length()-2));
        return wordDef;
    }

    /**
     * Set UI elements.
     * Sets the word, type, and definition for their respective textviews.
     * Also sets the current state for the star checkbox and the scroll position of the scrollview.
     *
     * @param def -- The definition to be set in the textview.
     */
    private void setDefinition(Definition def) {
        //set text to textviews
        word.setText(def.getWord());
        type.setText(def.getType());
        defn.setText(def.getDefn());

        checkFavorites();
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //scroll to top once the layout is ready
                /*scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);*/
                scrollView.smoothScrollTo(0, 0);
            }
        });
    }

    /**
     * Performs the process of displaying a definition.
     * Displays the definition of a word, which would then be added to history.
     * Then, update the list of previous words.
     *
     * @param current -- The current definition to display.
     * @param isSearch -- Checks if the word has been searched.
     */
    public void show(Definition current, int isSearch) {
        setDefinition(current);
        db.addtoHistory(current.getWord(), isSearch);
        updateHistory();
    }
}