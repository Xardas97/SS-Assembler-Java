package com.endava.mmarko;

import java.util.LinkedList;
import java.util.List;

class LineParser {
  enum LineType { EMPTY, SECTION, DIRECTIVE, INSTRUCTION }
  enum SectionType { TEXT, DATA, BSS, SECTION;
    @Override public String toString() { return super.toString().toLowerCase(); } }
  enum DirectiveType { BYTE, WORD, ALIGN, SKIP, EXTERN, GLOBAL, EQU;
    @Override public String toString() { return super.toString().toLowerCase(); }
  }

  private String label;
  private String sectionName;
  private String directiveName;
  private String instruction;

  private LineType type;

  private final List<Integer> values;
  private int value;
  private String symbol;
  private String sectionFlags;


  LineParser(String line, List<EquSymbol> equTable) throws SyntaxError {
    values = new LinkedList<>();
    label = sectionName = directiveName = instruction = "";
    type = LineType.EMPTY;
    parse(line, equTable);
  }

  private void parse(String line, List<EquSymbol> equTable) throws SyntaxError {
    line = parseComment(line);
    line = parseLabel(line);

    if (line.isEmpty()) {
      type = LineType.EMPTY;
      return;
    }

    //if directive or section
    int pos = line.lastIndexOf('.');
    if (pos >= 0) {
      if (pos != 0) throw new SyntaxError("Unexpected '.' character");
      line = line.substring(1);

      boolean shouldLeave;
      shouldLeave = parseIfSection(line);
      if (shouldLeave) return;
      shouldLeave = parseIfDirective(line, equTable);
      if (shouldLeave) return;

      //if none but had a '.', throw syntax error
      throw new SyntaxError("Unknown Section or Directive");
    }

    //instruction
    type = LineType.INSTRUCTION;
    instruction = line;
  }

  private String parseComment(String line) {
    int pos = line.indexOf(';');
    if (pos >= 0) {
      line = line.substring(0, pos);
    }
    return line;
  }

  private String parseLabel(String line) throws SyntaxError {
    String[] split = line.split(":");
    switch(split.length) {
      case 1: line = line.trim(); break;
      case 2: {
        label = split[0].trim();
        line = split[1].trim();
        //label can't be empty and can't contain spaces
        if (label.isEmpty() || label.contains(" \t")) throw new SyntaxError("Bad Label Name");
        break;
      }
      default: throw new SyntaxError("Unexpected Character: ':'");
    }
    return line;
  }

  private boolean parseIfSection(String line) throws SyntaxError {
    for (SectionType section : SectionType.values()) {
      if (line.indexOf(section.toString()) == 0) {
        type = LineType.SECTION;
        sectionName = section.toString();
        switch (section) {
          case SECTION:
            line = line.substring(sectionName.length());
            ParameterHelper parameterHelper = new ParameterHelper(line);
            sectionName = parameterHelper.getParam(0);
            if (sectionName.length() > 23) throw new SyntaxError("Symbol name too long, max characters: 23");
            sectionFlags = parameterHelper.getParam(1);
            break;
          case TEXT: sectionFlags = "rx";  break;
          case DATA: sectionFlags = "rw"; break;
          case BSS: sectionFlags = "r"; break;
        }
        return true;
      }
    }
    return false;
  }

  private boolean parseIfDirective(String line, List<EquSymbol> equTable) throws SyntaxError {
    for (DirectiveType directive : DirectiveType.values())
      if (line.indexOf(directive.toString()) == 0) {
        type = LineType.DIRECTIVE;
        directiveName = directive.toString();
        line = line.substring(directiveName.length());
        ParameterHelper parameterHelper = new ParameterHelper(line);
        switch (directive) {
          case WORD: case BYTE: {
            for (String param : parameterHelper.getParams()) {
              int value = Integer.parseInt(param);
              for (EquSymbol sym : equTable)
                if (param.equals(sym.getLabel())) {
                  value = sym.getValue();
                  break;
                }
              values.add(value);
            }
            break;
          }
          case SKIP: case ALIGN: value = Integer.parseInt(parameterHelper.getParam(0)); break;
          case EQU: {
            if ("".equals(parameterHelper.getParam(1))) throw new SyntaxError("Missing Parameter");
            symbol = parameterHelper.getParam(0);
            value = Integer.parseInt(parameterHelper.getParam(1));
            if (symbol.length() > 23) throw new SyntaxError("Symbol name too long, max characters: 23");
            break;
          }
          case EXTERN: case GLOBAL: symbol = parameterHelper.getParam(0); break;
        }
        return true;
      }
    return false;
  }

  String getLabel() {
    return label;
  }

  String getSectionName() {
    return sectionName;
  }

  String getDirectiveName() {
    return directiveName;
  }

  String getInstruction() {
    return instruction;
  }

  LineType getType() {
    return type;
  }

  List<Integer> getValues() {
    return values;
  }

  int getValue() {
    return value;
  }

  String getSymbol() {
    return symbol;
  }

  String getSectionFlags() {
    return sectionFlags;
  }
}
