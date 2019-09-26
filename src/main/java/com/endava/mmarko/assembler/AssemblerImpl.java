package com.endava.mmarko.assembler;

import com.endava.mmarko.assembler.parsers.InstructionParser;
import com.endava.mmarko.assembler.parsers.InstructionParser.ParsedInstructionData;
import com.endava.mmarko.assembler.parsers.LineParser;
import com.endava.mmarko.assembler.parsers.LineParser.ParsedLineData;
import com.endava.mmarko.assembler.parsers.NumberParser;
import com.endava.mmarko.assembler.tables.EquSymbol;
import com.endava.mmarko.assembler.tables.RelocationSymbol;
import com.endava.mmarko.assembler.tables.SectionTable;
import com.endava.mmarko.assembler.tables.SymbolTable;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssemblerImpl implements Assembler {
  private final LineParser lineParser;
  private final InstructionParser instrParser;
  private final NumberParser numberParser;

  static class Tables {
    final SymbolTable symbolTable = new SymbolTable();
    final SectionTable sectionTable = new SectionTable();
    final List<EquSymbol> equTable = new LinkedList<>();
  }

  @Autowired
  public AssemblerImpl(LineParser lineParser, InstructionParser instrParser, NumberParser numberParser) {
    this.lineParser = lineParser;
    this.instrParser = instrParser;
    this.numberParser = numberParser;
  }

  @Override
  public void assemble(String inputFileName, String outputFileName) throws IOException, SyntaxError {
    Tables tables = new Tables();

    List<String> firstPassOutput = firstPass(inputFileName, tables);
    secondPass(firstPassOutput, outputFileName, tables);
  }

  @Override
  public List<String> firstPass(String inputFileName, Tables tables) throws IOException, SyntaxError {
    try (BufferedReader input = new BufferedReader(new FileReader(new File(inputFileName)))) {

      List<String> output =  new LinkedList<>();
      SectionLocation location = new SectionLocation();

      String line = input.readLine();

      while (line != null && !line.contains(".end")) {
        if (line.isBlank()) {
          line = input.readLine();
          continue;
        }

        line = removeCarriageReturn(line);

        try {
          ParsedLineData data = lineParser.parse(line, tables.equTable);

          addLabel(data.label, location, tables.symbolTable);

          switch (data.type) {
            case SECTION: {
              location.incrementSection();
              tables.sectionTable.add(data.sectionName, location.getId(), data.sectionFlags);
              break;
            }
            case DIRECTIVE: firstPassDirective(data, location, tables); break;
            case INSTRUCTION: firstPassInstruction(data.instruction, location); break;
            default: break;
          }
        } catch (SyntaxError e) {
          e.setLine(line);
          throw e;
        }

        output.add(line);
        line = input.readLine();
      }

      return output;
    }
  }

  @Override
  public void secondPass(List<String> input, String outputFileName, Tables tables) throws IOException, SyntaxError {
    try (BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFileName)))) {
      SectionInfo sectionInfo = new SectionInfo();
      StringBuilder tempOutput = new StringBuilder();

      for (String line: input) {
        try {
          ParsedLineData data = lineParser.parse(line, tables.equTable);

          switch (data.type) {
            case SECTION: {
              printSection(sectionInfo, tempOutput);
              sectionInfo.startNewSection(data.sectionName);
              String outputFormatting = "======"
                  + sectionInfo.getName()
                  + "===================================================================================================";
              tempOutput.append("\n").append(outputFormatting, 0, 105).append("\n");
              break;
            }
            case DIRECTIVE: secondPassDirective(data, sectionInfo, tables.symbolTable); break;
            case INSTRUCTION: secondPassInstruction(data.instruction, sectionInfo, tables); break;
            default: break;
          }
        } catch (SyntaxError e) {
          e.setLine(line);
          throw e;
        }
      }

      printSection(sectionInfo, tempOutput);
      tempOutput.append("\n\n\n");

      output.write(tables.symbolTable.toString());
      output.write(tables.sectionTable.toString());
      output.write(tempOutput.toString());
    }
  }

  private String removeCarriageReturn(String line) {
    if (line.charAt(line.length() - 1) == 13) { // 13 - carriage return ascii code
      line = line.substring(0, line.length() - 1);
    }
    return line;
  }

  private void addLabel(String label, SectionLocation location, SymbolTable symbolTable) throws SyntaxError {
    if (!label.isEmpty()) {
      if (location.isInvalid()) throw new SyntaxError("Not in a Section");
      if (label.length() > 23) throw new SyntaxError("Label too long, max characters: 23");
      symbolTable.add(label, location);
    }
  }

  private void firstPassDirective(ParsedLineData data, SectionLocation location, Tables tables) {
    switch (data.directiveName) {
      case "byte":
        location.incrementOffset(data.values.size()); break;
      case "word": location.incrementOffset(2 * data.values.size()); break;
      case "skip": location.incrementOffset(data.values.get(0)); break;
      case "align": while (location.getOffset() % data.values.get(0) != 0) location.incrementOffset(); break;
      case "equ":
        tables.symbolTable.add(data.symbol, location);
        tables.equTable.add(new EquSymbol(data.symbol, data.values.get(0)));
        break;
      case "extern": tables.symbolTable.add(data.symbol, new SectionLocation(), true);
    }
  }

  private void firstPassInstruction(String instruction, SectionLocation location) throws SyntaxError {
    if (location.isInvalid()) throw new SyntaxError("Not in a Section");
    int instrSize = instrParser.parse(instruction, location.getOffset()).instrSize;
    location.incrementOffset(instrSize);
  }

  private void secondPassDirective(ParsedLineData data, SectionInfo info, SymbolTable symbolTable) throws SyntaxError {
    List<Integer> values = data.values;
    switch (data.directiveName) {
      case "byte":
        info.incrementOffset(values.size());
        for (int value: values) info.append(numberParser.toHex(value, 1));
        break;
      case "word":
        info.incrementOffset(2 * values.size());
        for (int value : values) info.append(numberParser.swapBytes(numberParser.toHex(value, 2)));
        break;
      case "skip":
        info.append("00".repeat(Math.max(0, data.values.get(0))));
        info.incrementOffset(data.values.get(0));
        break;
      case "align":
        while (info.getOffset() % data.values.get(0) != 0) {
          info.incrementOffset();
          info.append("00");
        }
        break;
      case "global": symbolTable.setGlobal(data.symbol); break;
    }
  }

  private void secondPassInstruction(String instruction, SectionInfo info, Tables tables) throws SyntaxError {
    if (info == null) return;

    ParsedInstructionData data = instrParser.parse(instruction, info.getOffset());

    info.incrementOffset(data.instrSize);

    long instrCode = instrParser.createInstrCode(data, tables.symbolTable, tables.equTable, info.getRelocationTable());
    String instrCodeHex = numberParser.toHex(instrCode, data.instrSize);
    info.append(instrCodeHex);
  }

  private void printSection(SectionInfo info, StringBuilder tempOutput) {
    if (info.isValid()) {
      String formattedBytes = numberParser.format(info.getBytes());
      tempOutput.append(formattedBytes);
      printRelocationTable(info, tempOutput);
    }
  }

  private void printRelocationTable(SectionInfo info, StringBuilder output) {
    if (info.getRelocationTable() == null) return;
    String outputFormatting = "======.rel " + info.getName() + "===================================================================================================";
    output.append("\n").append(outputFormatting, 0, 105).append("\n");
    output.append("      Offset            Type          Section         Symbol\n");
    output.append("---------------------------------------------------------------------------------------------------------\n");
    for (RelocationSymbol rs : info.getRelocationTable()) output.append(rs.toString()).append("\n");
    output.append("=========================================================================================================");
  }

}
