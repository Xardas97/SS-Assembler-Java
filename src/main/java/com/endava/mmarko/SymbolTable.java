package com.endava.mmarko;

import java.util.LinkedList;
import java.util.List;

class SymbolTable {
  private final List<Symbol> symbols;

  SymbolTable() {
    symbols = new LinkedList<>();
  }

  void add(String label, SectionLocation location) {
    add(label, location, false);
  }

  void add(String label, SectionLocation location, boolean global) {
    symbols.add(new Symbol(label, location, global));
  }

  void setGlobal(String symbol) throws SyntaxError {
    for (Symbol s : symbols) {
      if (s.getLabel().equals(symbol)) {
        s.setGlobal();
        return;
      }
    }
    throw new SyntaxError("Symbol not defined");
  }

  Symbol find(String label) {
    for (Symbol s : symbols) {
      if (s.getLabel().equals(label)) return s;
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder ret;

    ret = new StringBuilder("==============================================SYMBOL TABLE===============================================\n"
        + "      Name            Section                     Offset                         G/L\n"
        + "---------------------------------------------------------------------------------------------------------\n");
    for (Symbol s : symbols)
      ret.append(s).append("\n");
    ret.append("=========================================================================================================\n");

    return ret.toString();
  }

}
