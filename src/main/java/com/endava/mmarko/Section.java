package com.endava.mmarko;

class Section {
  private final String label;
  private final int id;
  private final String flag;

  Section(String label, int id, String flag) {
    this.label = label;
    this.id = id;
    this.flag = flag;
  }

  @Override
  public String toString() {
    String tabs = "\t\t\t";
    StringBuilder tabsName = new StringBuilder();

    for (int i = 0; i <= (23 - label.length()); i += 8)
      tabsName.append("\t");

    return label + tabsName + id + tabs + flag;
  }
}
