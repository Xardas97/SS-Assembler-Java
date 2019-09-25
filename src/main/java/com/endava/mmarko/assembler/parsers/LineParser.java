package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SyntaxError;
import com.endava.mmarko.assembler.tables.EquSymbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class LineParser {
  public enum LineType { EMPTY, SECTION, DIRECTIVE, INSTRUCTION }
  enum SectionType { TEXT, DATA, BSS, SECTION;
    @Override public String toString() { return super.toString().toLowerCase(); } }
  enum DirectiveType { BYTE, WORD, ALIGN, SKIP, EXTERN, GLOBAL, EQU;
    @Override public String toString() { return super.toString().toLowerCase(); }
  }

  private final NumberParser numberParser;
  private final ParameterParser parameterParser;

  @Autowired
  public LineParser(NumberParser numberParser, ParameterParser parameterParser) {
    this.numberParser = numberParser;
    this.parameterParser = parameterParser;
  }

  public static class ParsedLineData {
    ParsedLineData() {
      values = new LinkedList<>();
      label = sectionName = directiveName = instruction = "";
      type = LineType.EMPTY;
    }

    public String label;
    public String sectionName;
    public String directiveName;
    public String instruction;

    public LineType type;

    public final List<Integer> values;
    public String symbol;
    public String sectionFlags;
  }

  public ParsedLineData parse(String line, List<EquSymbol> equTable) throws SyntaxError {
    ParsedLineData data = new ParsedLineData();

    line = parseComment(line);
    line = parseLabel(line, data);

    if (line.isEmpty()) {
      return data;
    }

    //if directive or section
    int pos = line.lastIndexOf('.');
    if (pos >= 0) {
      if (pos != 0) throw new SyntaxError("Unexpected '.' character");
      line = line.substring(1);

      boolean shouldLeave;
      shouldLeave = parseIfSection(line, data);
      if (shouldLeave) return data;
      shouldLeave = parseIfDirective(line, equTable, data);
      if (shouldLeave) return data;

      //if none but had a '.', throw syntax error
      throw new SyntaxError("Unknown Section or Directive");
    }

    //instruction
    data.type = LineType.INSTRUCTION;
    data.instruction = line;

    return data;
  }

  private String parseComment(String line) {
    int pos = line.indexOf(';');
    if (pos >= 0) {
      line = line.substring(0, pos);
    }
    return line;
  }

  private String parseLabel(String line, ParsedLineData data) throws SyntaxError {
    if (line.contains(":")) {
      String[] split = line.split(":");
      if (split.length == 1 || split.length == 2) {
        if (split.length == 2) {
          line = split[1];
        } else {
          line = "";
        }
        data.label = split[0].trim();
        //label can't be empty and can't contain spaces
        if (data.label.isEmpty() || data.label.contains(".") ||
            data.label.contains(" ") || data.label.contains("\t")) {
          throw new SyntaxError("Bad Label Name");
        }
      }
      else {
        throw new SyntaxError("Unexpected Character: ':'");
      }
    }
    return line.trim();
  }

  private boolean parseIfSection(String line, ParsedLineData data) throws SyntaxError {
    for (SectionType section : SectionType.values()) {
      if (line.indexOf(section.toString()) == 0) {
        data.type = LineType.SECTION;
        data.sectionName = section.toString();
        switch (section) {
          case SECTION:
            line = line.substring(data.sectionName.length());
            List<String> params = parameterParser.parse(line);
            data.sectionName = params.get(0);
            if (params.size() != 2) {
              throw new SyntaxError("Wrong number of parameters, expected: 2");
            }
            if (data.sectionName.length() > 23) {
              throw new SyntaxError("Symbol name too long, max characters: 23");
            }
            data.sectionFlags = params.get(1);
            break;
          case TEXT: data.sectionFlags = "rx";  break;
          case DATA: data.sectionFlags = "rw"; break;
          case BSS: data.sectionFlags = "r"; break;
        }
        return true;
      }
    }
    return false;
  }

  private boolean parseIfDirective(String line, List<EquSymbol> equTable, ParsedLineData data)
      throws SyntaxError {
    for (DirectiveType directive : DirectiveType.values())
      if (line.indexOf(directive.toString()) == 0) {
        data.type = LineType.DIRECTIVE;
        data.directiveName = directive.toString();
        line = line.substring(data.directiveName.length());
        List<String> params = parameterParser.parse(line);
        switch (directive) {
          case WORD: case BYTE: {
            for (String param : params) {
              int value = 0;
              try {
                value = numberParser.parseInt(param);
              }
              catch(NumberFormatException ignored) { }
              for (EquSymbol sym : equTable)
                if (param.equals(sym.getLabel())) {
                  value = sym.getValue();
                  break;
                }
              data.values.add(value);
            }
            break;
          }
          case SKIP: case ALIGN:
            data.values.add(numberParser.parseInt(params.get(0))); break;
          case EQU: {
            if ("".equals(params.get(1))) throw new SyntaxError("Missing Parameter");
            data.symbol = params.get(0);
            data.values.add(numberParser.parseInt(params.get(1)));
            if (data.symbol.length() > 23) throw new SyntaxError("Symbol name too long, max characters: 23");
            break;
          }
          case EXTERN: case GLOBAL: data.symbol = params.get(0); break;
        }
        return true;
      }
    return false;
  }
}
