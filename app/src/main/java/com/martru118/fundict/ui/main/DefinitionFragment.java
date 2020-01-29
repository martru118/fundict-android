package com.martru118.fundict.ui.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.martru118.fundict.Helper.DatabaseOpenHelper;
import com.martru118.fundict.MainActivity;
import com.martru118.fundict.Model.Definition;
import com.martru118.fundict.R;

import java.util.List;
import java.util.Locale;

public class DefinitionFragment extends Fragment {
    private TextToSpeech pronunciation;
    private TextView word, type, defn;
    private LinearLayout buttonsPanel;
    private CheckBox star;

    private List<Definition> result;
    private DatabaseOpenHelper db;

    public DefinitionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseOpenHelper(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frame_definition, container, false);
        word = v.findViewById(R.id.word);
        type = v.findViewById(R.id.type);
        defn = v.findViewById(R.id.definition);

        buttonsPanel = v.findViewById(R.id.buttonPanel);
        buttonsPanel.setVisibility(View.GONE);
        star = v.findViewById(R.id.fav_action);

        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String favWord, favType, favDefn;
                favWord = word.getText().toString();
                favType = type.getText().toString();
                favDefn = defn.getText().toString();

                if (!star.isChecked()) {
                    db.clicktoRemove(favWord, favType, favDefn);
                    makeToast("Removed from bookmarks");
                } else {
                    db.addtoFavorites(favWord, favType, favDefn);
                    makeToast("Added to bookmarks");
                }
            }
        });

        //enable text to speech
        ImageButton t2s = v.findViewById(R.id.tts);
        pronunciation = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status!=TextToSpeech.ERROR) {
                    pronunciation.setLanguage(Locale.US);
                }
            }
        });
        t2s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence toSpeak = word.getText();
                pronunciation.setSpeechRate(0.65f);
                pronunciation.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        //copy to clipboard
        ImageButton copy = v.findViewById(R.id.copy);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("word", word.getText());
                clipboard.setPrimaryClip(clip);

                makeToast("Word copied to clipboard");
            }
        });

        //set floating action button
        FloatingActionButton fab = v.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //generate random definition
                String key = "generateRandom()";
                buttonsPanel.setVisibility(View.VISIBLE);

                //get definition
                result = db.findDefinition(key);
                Definition wordDef = result.get(0);
                changeText(wordDef);

                //update search history
                db.addSearch(wordDef.getWord());
                ((MainActivity)getActivity()).updateHistory();
            }
        });
        fab.setAlpha(0.65f);

        return v;
    }

    @Override
    public void onDestroyView() {
        pronunciation.shutdown();
        super.onDestroyView();
    }

    private void makeToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void changeText(Definition def) {
        word.setText(def.getWord());
        type.setText(def.getType());
        defn.setText(def.getDefn());

        buttonsPanel.setVisibility(View.VISIBLE);
        checkFavorites();
    }

    private void checkFavorites() {
        if (db.isFavorite(word.getText().toString(), type.getText().toString(), defn.getText().toString()))
            star.setChecked(true);
        else
            star.setChecked(false);
    }
}