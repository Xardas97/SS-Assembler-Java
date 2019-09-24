package com.endava.mmarko.assembler.tables;

import java.util.LinkedList;
import java.util.List;

public class SectionTable {
  private final List<Section> sections;

  public SectionTable() {
    sections = new LinkedList<>();
  }

  public void add(String label, int section, String flags) {
    sections.add(new Section(label, section, flags));
  }

  @Override
  public String toString() {
    StringBuilder ret;

    ret = new StringBuilder(
        "==============================================SECTION TABLE==============================================\n"
        + "   Name              Section                   Flags\n"
        + "---------------------------------------------------------------------------------------------------------\n");

    for (Section s : sections)
      ret.append(s).append("\n");

    ret.append("=========================================================================================================");

    return ret.toString();
  }
}
