package com.endava.mmarko.assembler.tables;

import com.endava.mmarko.assembler.SectionLocation;
import com.endava.mmarko.assembler.SyntaxError;

import java.util.LinkedList;
import java.util.List;

public class SymbolTable {
  private final List<Symbol> symbols;

  public SymbolTable() {
    symbols = new LinkedList<>();
  }

  public void add(String label, SectionLocation location) {
    add(label, location, false);
  }

  public void add(String label, SectionLocation location, boolean global) {
    symbols.add(new Symbol(label, location, global));
  }

  public void setGlobal(String symbol) throws SyntaxError {
    for (Symbol s : symbols) {
      if (s.getLabel().equals(symbol)) {
        s.setGlobal();
        return;
      }
    }
    throw new SyntaxError("Symbol not defined");
  }

  public Symbol find(String label) {
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
