package com.endava.mmarko.assembler.tables;

import com.endava.mmarko.assembler.parsers.NumberParser;

public class RelocationSymbol {
  private final int offset;
  private final String type;
  private final int section;
  private final String symbol;

  public RelocationSymbol(int offset, String type, int section, String symbol) {
    this.offset = offset;
    this.type = type;
    this.section = section;
    this.symbol = symbol;
  }

  @Override
  public String toString() {
    String tabs = "\t\t";

    String ret = "    " + NumberParser.toHexStatic(offset, 5) + tabs + type + tabs;
    if (section >= 0) {
      ret += Integer.toString(section);
    }
    else {
      ret += "und";
    }
    ret += (tabs + symbol);

    return ret;
  }
}
