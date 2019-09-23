package com.endava.mmarko;

import static com.endava.mmarko.NumberParser.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

class AssemblerImpl implements Assembler {
  private BufferedReader input;
  private BufferedWriter output;

  private SymbolTable symbolTable;
  private SectionTable sectionTable;
  private List<EquSymbol> equTable;

  AssemblerImpl(String input, String output) throws IOException {
    this.input = new BufferedReader(new FileReader(new File(input)));
    this.output = new BufferedWriter(new FileWriter(new File(output)));
    symbolTable = new SymbolTable();
    sectionTable = new SectionTable();
    equTable = new LinkedList<>();
  }

  @Override
  public List<String> firstPass() throws IOException {
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
        LineParser lineParser = new LineParser(line, equTable);

        addLabel(lineParser.getLabel(), location);

        switch (lineParser.getType()) {
          case SECTION: {
            location.incrementSection();
            sectionTable.add(lineParser.getSectionName(), location.getId(), lineParser.getSectionFlags());
            break;
          }
          case DIRECTIVE: firstPassDirective(lineParser, location); break;
          case INSTRUCTION: firstPassInstruction(lineParser, location); break;
        }
      } catch (SyntaxError e) {
        System.out.println( "SYNTAX ERROR in line: " + line + "\n");
        System.out.println(e.getMessage() + "\n");
        System.exit(-2);
      }

      output.add(line);
      line = input.readLine();
    }

    return output;
  }

  @Override
  public void secondPass(List<String> input) throws IOException {
    SectionInfo sectionInfo = new SectionInfo();
    StringBuilder tempOutput = new StringBuilder();

    for(String line: input) {
      try {
        LineParser lineParser = new LineParser(line, equTable);

        switch(lineParser.getType()) {
          case SECTION: {
            printSection(sectionInfo, tempOutput);
            sectionInfo.startNewSection(lineParser.getSectionName());
            String outputFormatting = "======"
                + sectionInfo.getName()
                + "===================================================================================================";
            tempOutput.append("\n").append(outputFormatting, 0, 105).append("\n");
            break;
          }
          case DIRECTIVE: secondPassDirective(lineParser, sectionInfo); break;
          case INSTRUCTION: secondPassInstruction(lineParser, sectionInfo); break;
        }
      } catch (SyntaxError e) {
        System.out.println( "SYNTAX ERROR in line: " + line + "\n");
        System.out.println(e.getMessage() + "\n");
        System.exit(-2);
      }
    }

    printSection(sectionInfo, tempOutput);
    tempOutput.append("\n\n\n");

    output.write(symbolTable.toString());
    output.write(sectionTable.toString());
    output.write(tempOutput.toString());
  }

  @Override
  public void close() throws IOException {
    input.close();
    output.close();
  }

  private String removeCarriageReturn(String line) {
    if (line.charAt(line.length() - 1) == 13) { // 13 - carriage return ascii code
      line = line.substring(0, line.length() -1);
    }
    return line;
  }

  private void addLabel(String label, SectionLocation location) throws SyntaxError {
    if (!label.isEmpty()) {
      if (location.isInvalid()) throw new SyntaxError("Not in a Section");
      if (label.length() > 23) throw new SyntaxError("Label too long, max characters: 23");
      symbolTable.add(label, location);
    }
  }

  private void firstPassInstruction(LineParser lineParser, SectionLocation location) throws SyntaxError {
    if (location.isInvalid()) throw new SyntaxError("Not in a Section");
    String instruction = lineParser.getInstruction();
    InstructionParser instrParser = new InstructionParser(instruction, location.getOffset());
    location.incrementOffset(instrParser.getInstrSize());
  }

  private void firstPassDirective(LineParser lineParser, SectionLocation location) {
    switch(lineParser.getDirectiveName()) {
      case "byte":
        location.incrementOffset(lineParser.getValues().size()); break;
      case "word": location.incrementOffset(2 * lineParser.getValues().size()); break;
      case "skip": location.incrementOffset(lineParser.getValue()); break;
      case "align": while (location.getOffset() % lineParser.getValue() != 0) location.incrementOffset(); break;
      case "equ":
        symbolTable.add(lineParser.getSymbol(), location);
        equTable.add(new EquSymbol(lineParser.getSymbol(), lineParser.getValue()));
        break;
      case "extern": symbolTable.add(lineParser.getSymbol(), new SectionLocation(), true);
    }
  }

  private void secondPassDirective(LineParser lineParser, SectionInfo info) throws SyntaxError {
    List<Integer> values = lineParser.getValues();
    switch(lineParser.getDirectiveName()){
      case "byte":
        info.incrementOffset(values.size());
        for(int value: values) info.append(toHex(value, 1));
        break;
      case "word":
        info.incrementOffset(2 * values.size());
        for (int value : values) info.append(swapBytes(toHex(value, 2)));
        break;
      case "skip":
        info.append("00".repeat(Math.max(0, lineParser.getValue())));
        info.incrementOffset(lineParser.getValue());
        break;
      case "align":
        while (info.getOffset() % lineParser.getValue() != 0) {
          info.incrementOffset();
          info.append("00");
        }
        break;
      case "global": symbolTable.setGlobal(lineParser.getSymbol()); break;
    }
  }

  private void secondPassInstruction(LineParser lineParser, SectionInfo info) throws SyntaxError {
    if(info == null) return;

    String instruction = lineParser.getInstruction();
    InstructionParser instrParser = new InstructionParser(instruction, info.getOffset());
    info.incrementOffset(instrParser.getInstrSize());

    long instrCode = instrParser.createInstrCode(symbolTable, equTable, info.getRelocationTable());
    String instrCodeHex = toHex(instrCode, instrParser.getInstrSize());
    info.append(instrCodeHex);
  }

  private void printSection(SectionInfo info, StringBuilder tempOutput) {
    if (info.isValid()) {
      String formattedBytes = format(info.getBytes());
      tempOutput.append(formattedBytes);
      printRelocationTable(info, tempOutput);
    }
  }

  private void printRelocationTable(SectionInfo info, StringBuilder output) {
    if(info.getRelocationTable() == null) return;
    String outputFormatting = "======.rel " + info.getName() + "===================================================================================================";
    output.append("\n").append(outputFormatting, 0, 105).append("\n");
    output.append("      Offset            Type          Section         Symbol\n");
    output.append("---------------------------------------------------------------------------------------------------------\n");
    for (RelocationSymbol rs : info.getRelocationTable()) output.append(rs.toString()).append("\n");
    output.append("=========================================================================================================");
  }

}
