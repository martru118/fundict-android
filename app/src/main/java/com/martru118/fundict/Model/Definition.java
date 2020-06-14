package com.martru118.fundict.Model;

/**
 * Model class for definition.
 * A definition has a word, a type, and a defn (description).
 */
public class Definition {
    private String word, type, defn;

    public Definition() {}

    public String getWord() {return word;}
    public String getType() {return type;}
    public String getDefn() {return defn;}

    public void setWord(String word) {this.word = word;}
    public void setType(String type) {this.type = type;}
    public void setDefn(String defn) {this.defn = defn;}
}
