package eu.interedition.collatex2.interfaces;

public interface IOmission extends IModification {

  int getPosition();

  IColumns getOmittedColumns();

}