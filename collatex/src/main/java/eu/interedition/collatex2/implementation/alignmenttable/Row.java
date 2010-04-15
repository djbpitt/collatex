package eu.interedition.collatex2.implementation.alignmenttable;

import java.util.Iterator;
import java.util.List;

import eu.interedition.collatex2.interfaces.ICell;
import eu.interedition.collatex2.interfaces.IRow;

public class Row implements IRow {
  private List<ICell> cells;
  private final String sigil;

  public Row(String sigil, List<ICell> cells) {
    this.sigil = sigil;
    this.cells = cells;
  }

  @Override
  public Iterator<ICell> iterator() {
    return cells.iterator();
  }

  @Override
  public String getSigil() {
    return sigil;
  }
}
