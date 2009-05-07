package com.sd_editions.collatex.permutations;

import java.util.List;
import java.util.Random;

import com.google.common.base.Join;
import com.google.common.collect.Lists;

public class Witness {
  public final String sentence;
  public final String id;
  private final List<Word> words;

  public Witness(Word... _words) {
    if (_words == null) throw new IllegalArgumentException("List of words cannot be null.");
    if (_words.length == 0)
      this.id = Long.toString(Math.abs(new Random().nextLong()), 5);
    else
      this.id = _words[0].getWitnessId();
    this.sentence = Join.join(" ", _words);
    this.words = Lists.newArrayList(_words);
  }

  public List<Word> getWords() {
    return words;
  }

  public Word getWordOnPosition(int position) {
    return words.get(position - 1);
  }

  public int size() {
    return words.size();
  }
}
