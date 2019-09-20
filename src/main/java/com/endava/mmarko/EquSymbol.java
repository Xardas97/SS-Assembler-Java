package com.endava.mmarko;

class EquSymbol {
  private final String label;
  private final int value;

  EquSymbol(String label, int value) {
    this.label = label;
    this.value = value;
  }

  @Override
  public String toString() {
    return label + "\t\t\t" + value + "\t\t\t";
  }

  String getLabel() {
    return label;
  }

  int getValue() {
    return value;
  }
}
