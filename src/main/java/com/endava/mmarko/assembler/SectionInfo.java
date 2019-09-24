package com.endava.mmarko.assembler;

import com.endava.mmarko.assembler.tables.RelocationSymbol;

import java.util.LinkedList;
import java.util.List;

class SectionInfo extends SectionLocation {
  private List<RelocationSymbol> relocationTable;
  private String name;
  private StringBuilder bytes;

  void startNewSection(String name) {
    super.incrementSection();
    this.name = name;
    relocationTable = new LinkedList<>();
    bytes = new StringBuilder();
  }

  void append(String s) {
    bytes.append(s);
  }

  List<RelocationSymbol> getRelocationTable() {
    return relocationTable;
  }

  String getName() {
    return name;
  }

  String getBytes() {
    return bytes.toString();
  }
}
