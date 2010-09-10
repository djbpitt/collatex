package eu.interedition.collatex2.legacy.tokencontainers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.interedition.collatex2.interfaces.IAlignmentTable;
import eu.interedition.collatex2.interfaces.IColumn;
import eu.interedition.collatex2.interfaces.IColumns;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.IPhrase;
import eu.interedition.collatex2.interfaces.ITokenIndex;
import eu.interedition.collatex2.legacy.alignmenttable.Columns;
import eu.interedition.collatex2.legacy.indexing.ColumnPhrase;
import eu.interedition.collatex2.legacy.indexing.NullColumn;

//TODO: legacy class REMOVE! REMOVE!
public class AlignmentTableIndex implements ITokenIndex {
  private static Logger logger = LoggerFactory.getLogger(AlignmentTableIndex.class);

  //TODO: rename field normalizedToColumns!
  private final Map<String, ColumnPhrase> normalizedToColumns;

  private AlignmentTableIndex() {
    this.normalizedToColumns = Maps.newLinkedHashMap();
  }

  public static ITokenIndex create(final IAlignmentTable table, final List<String> repeatingTokens) {
    final AlignmentTableIndex index = new AlignmentTableIndex();
    for (final String sigil : table.getSigli()) {
      findUniquePhrasesForRow(sigil, table, index, repeatingTokens);
    }
    return index;
  }

  private static void findUniquePhrasesForRow(final String row, final IAlignmentTable table, final AlignmentTableIndex index, final List<String> findRepeatingTokens) {
    // filteren would be nicer.. maar we doen het maar even alles in een!
    for (final IColumn column : table.getColumns()) {
      if (column.containsWitness(row)) {
        final INormalizedToken token = column.getToken(row);
        // kijken of ie unique is
        final boolean unique = !findRepeatingTokens.contains(token.getNormalized());
        if (unique) {
          final IColumns columns = new Columns(Lists.newArrayList(column));
          final ColumnPhrase phrase = new ColumnPhrase(token.getNormalized(), columns, Lists.newArrayList(row));
          index.add(phrase);
        } else {
          //System.out.println("We have to combine stuff here!");
          final ColumnPhrase leftPhrase = findUniqueColumnPhraseToTheLeft(table, findRepeatingTokens, row, column, token);
          final ColumnPhrase rightPhrase = findUniqueColumnPhraseToTheRight(table, findRepeatingTokens, row, column, token);
          index.add(leftPhrase);
          index.add(rightPhrase);
        }
      } /*else {
                  logger.debug("Column " + column.getPosition() + " is empty!");
                }*/
    }
  }

  //TODO: add test support of empty cells!
  private static ColumnPhrase findUniqueColumnPhraseToTheLeft(final IAlignmentTable table, final List<String> findRepeatingTokens, final String row, final IColumn column, final INormalizedToken token) {
    // combine to the left
    final ColumnPhrase phrase = new ColumnPhrase(token.getNormalized(), new Columns(Lists.newArrayList(column)), Lists.newArrayList(row));
    boolean found = false; // not nice!
    for (int i = column.getPosition() - 1; !found && i > 0; i--) {
      final IColumn leftColumn = table.getColumns().get(i - 1);
      final boolean empty = !leftColumn.containsWitness(row);
      if (!empty) {
        final String normalizedNeighbour = leftColumn.getToken(row).getNormalized();
        found = !empty && !findRepeatingTokens.contains(normalizedNeighbour);
        phrase.addColumnToLeft(leftColumn);
      }
    }
    if (!found) {
      phrase.addColumnToLeft(new NullColumn(1));
    }
    return phrase;
  }

  //TODO: add test for support of empty cells!
  private static ColumnPhrase findUniqueColumnPhraseToTheRight(final IAlignmentTable table, final List<String> findRepeatingTokens, final String row, final IColumn column, final INormalizedToken token) {
    final ColumnPhrase phrase = new ColumnPhrase(token.getNormalized(), new Columns(Lists.newArrayList(column)), Lists.newArrayList(row));
    boolean found = false; // not nice!
    for (int i = column.getPosition() + 1; !found && i < table.size() + 1; i++) {
      final IColumn rightColumn = table.getColumns().get(i - 1);
      final boolean empty = !rightColumn.containsWitness(row);
      if (!empty) {
        final String normalizedNeighbour = rightColumn.getToken(row).getNormalized();
        found = !empty && !findRepeatingTokens.contains(normalizedNeighbour);
        phrase.addColumnToRight(rightColumn);
      }
    }
    if (!found) {
      phrase.addColumnToRight(new NullColumn(table.size()));
    }
    return phrase;
  }

  private void add(final ColumnPhrase phrase) {
    normalizedToColumns.put(phrase.getNormalized(), phrase);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("AlignmentTableIndex: (");
    String delimiter = "";
    for (final String normalizedPhrase : normalizedToColumns.keySet()) {
      result.append(delimiter).append(normalizedPhrase);
      delimiter = ", ";
    }

    result.append(")");
    return result.toString();
  }

  //NOTE: From this point on the IWitnessIndex methods start!
  @Override
  public boolean contains(String normalized) {
    System.out.println(normalizedToColumns.keySet());
    return normalizedToColumns.containsKey(normalized);
  }

  @Override
  public IPhrase getPhrase(String normalized) {
    ColumnPhrase columnPhrase = normalizedToColumns.get(normalized);
    return columnPhrase.getPhrase();
  }

  @Override
  public Set<String> keys() {
    return normalizedToColumns.keySet();
  }

  @Override
  public int size() {
    return normalizedToColumns.size();
  }
}