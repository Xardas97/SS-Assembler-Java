package com.endava.mmarko.assembler.tables;

public class EquSymbol {
  private final String label;
  private final int value;

  public EquSymbol(String label, int value) {
    this.label = label;
    this.value = value;
  }

  @Override
  public String toString() {
    return label + "\t\t\t" + value + "\t\t\t";
  }

  public String getLabel() {
    return label;
  }

  public int getValue() {
    return value;
  }
}
