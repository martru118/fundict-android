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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.Model.Definition;
import com.martru118.fundict.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DefinitionFragment extends Fragment {
    private TextToSpeech pronunciation;
    private TextView word, type, defn;
    private BottomAppBar buttonsPanel;

    private DatabaseOpenHelper db;

    private final String defaultKey = "welcome";
    private List<String> previous = new ArrayList<>();
    private int i_prev;
    private boolean isFavorite;

    public DefinitionFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseOpenHelper(getContext());
        updateHistory();
        i_prev = previous.size()-1;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frame_definition, container, false);
        word = v.findViewById(R.id.word);
        type = v.findViewById(R.id.type);
        defn = v.findViewById(R.id.definition);
        defn.scrollTo(0, defn.getTop());

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
                        makeToast("Cannot load Text-to-Speech service");
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

    private void setupButtonsPanel() {
        buttonsPanel.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    //back button
                    case R.id.back:
                        if (i_prev > -1) {
                            Definition prev = db.findDefinition(previous.get(i_prev), false);
                            setDefinition(prev);
                            i_prev--;
                        } else {
                            makeToast("End of history");
                        }
                        return true;

                    //star checkbox
                    case R.id.fav_action:
                        Definition wordDef = getDefinition();

                        if (isFavorite) {
                            db.removefromFavorites("word", wordDef.getWord());
                            item.setIcon(R.drawable.ic_star_border_white_24dp);
                            isFavorite = false;

                            makeToast("Removed from favorites");
                            return true;
                        } else {
                            db.addtoFavorites(wordDef);
                            item.setIcon(R.drawable.ic_star_24dp);
                            isFavorite = true;

                            makeToast("Added to favorites");
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

                        makeToast("Definition copied to clipboard");
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    //default toast layout
    private void makeToast(String message) {
        Toast alert = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        alert.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 200);
        alert.show();
    }

    private void updateHistory() {
        previous = db.getHistory(false);
        i_prev = previous.size()-2;
    }

    private void checkFavorites() {
        if (db.exists("favorites", getDefinition().getWord())) {
            buttonsPanel.getMenu().getItem(1).setIcon(R.drawable.ic_star_24dp);
            isFavorite = true;
        } else {
            buttonsPanel.getMenu().getItem(1).setIcon(R.drawable.ic_star_border_white_24dp);
            isFavorite = false;
        }
    }

    private Definition getDefinition() {
        Definition wordDef = new Definition();
        wordDef.setWord(word.getText().toString());
        wordDef.setType(type.getText().toString().replace("\n", "/"));
        wordDef.setDefn(defn.getText().toString());
        return wordDef;
    }

    private void setDefinition(Definition def) {
        word.setText(def.getWord());
        type.setText(def.getType());
        defn.setText(def.getDefn());
        checkFavorites();
    }

    //show the definition
    public void show(Definition current, int isSearch) {
        setDefinition(current);
        db.addtoHistory(current.getWord(), isSearch);
        updateHistory();
    }
}