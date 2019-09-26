package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SyntaxError;
import com.endava.mmarko.assembler.tables.EquSymbol;
import com.endava.mmarko.assembler.tables.Symbol;
import com.endava.mmarko.assembler.tables.SymbolTable;
import com.endava.mmarko.assembler.tables.RelocationSymbol;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InstructionParser {
  private static final Map<String, Integer> INSTRUCTION_MAP;
  private static final Map<String, Integer> ADDRESSING_MAP;
  private static final Map<String, Integer> REGISTER_MAP;
  private static final List<String> ZERO_PARAM_INSTRUCTIONS;
  private static final List<String> ONE_PARAM_INSTRUCTIONS;

  private NumberParser numberParser;
  private ParameterParser parameterParser;
  
  @Autowired
  public InstructionParser(NumberParser numberParser, ParameterParser parameterParser) {
    this.numberParser = numberParser;
    this.parameterParser = parameterParser;
  }
  
  static {
    INSTRUCTION_MAP = new HashMap<>();
    INSTRUCTION_MAP.put("halt", 0x01);
    INSTRUCTION_MAP.put("xchg", 0x02);
    INSTRUCTION_MAP.put("int", 0x03);
    INSTRUCTION_MAP.put("mov", 0x04);
    INSTRUCTION_MAP.put("add", 0x05);
    INSTRUCTION_MAP.put("sub", 0x06);
    INSTRUCTION_MAP.put("mul", 0x07);
    INSTRUCTION_MAP.put("div", 0x08);
    INSTRUCTION_MAP.put("cmp", 0x09);
    INSTRUCTION_MAP.put("not", 0x0A);
    INSTRUCTION_MAP.put("and", 0x0B);
    INSTRUCTION_MAP.put("or", 0x0C);
    INSTRUCTION_MAP.put("xor", 0x0D);
    INSTRUCTION_MAP.put("test", 0x0E);
    INSTRUCTION_MAP.put("shl", 0x0F);
    INSTRUCTION_MAP.put("shr", 0x10);
    INSTRUCTION_MAP.put("push", 0x11);
    INSTRUCTION_MAP.put("pop", 0x12);
    INSTRUCTION_MAP.put("jmp", 0x13);
    INSTRUCTION_MAP.put("jeq", 0x14);
    INSTRUCTION_MAP.put("jne", 0x15);
    INSTRUCTION_MAP.put("jgt", 0x16);
    INSTRUCTION_MAP.put("call", 0x17);
    INSTRUCTION_MAP.put("ret", 0x18);
    INSTRUCTION_MAP.put("iret", 0x19);

    ADDRESSING_MAP = new HashMap<>();
    ADDRESSING_MAP.put("imm", 0x0);
    ADDRESSING_MAP.put("reg", 0x1);
    ADDRESSING_MAP.put("regind", 0x2);
    ADDRESSING_MAP.put("regind8", 0x3);
    ADDRESSING_MAP.put("regind16", 0x4);
    ADDRESSING_MAP.put("mem", 0x5);

    REGISTER_MAP = new HashMap<>();
    REGISTER_MAP.put("r0", 0x0);
    REGISTER_MAP.put("r1", 0x1);
    REGISTER_MAP.put("r2", 0x2);
    REGISTER_MAP.put("r3", 0x3);
    REGISTER_MAP.put("r4", 0x4);
    REGISTER_MAP.put("r5", 0x5);
    REGISTER_MAP.put("r6", 0x6);
    REGISTER_MAP.put("sp", 0x6);
    REGISTER_MAP.put("r7", 0x7);
    REGISTER_MAP.put("pc", 0x7);
    REGISTER_MAP.put("psw", 0xF);

    ZERO_PARAM_INSTRUCTIONS = new LinkedList<>();
    ZERO_PARAM_INSTRUCTIONS.add("halt");
    ZERO_PARAM_INSTRUCTIONS.add("ret");
    ZERO_PARAM_INSTRUCTIONS.add("iret");

    ONE_PARAM_INSTRUCTIONS = new LinkedList<>();
    ONE_PARAM_INSTRUCTIONS.add("int");
    ONE_PARAM_INSTRUCTIONS.add("not");
    ONE_PARAM_INSTRUCTIONS.add("push");
    ONE_PARAM_INSTRUCTIONS.add("pop");
    ONE_PARAM_INSTRUCTIONS.add("jmp");
    ONE_PARAM_INSTRUCTIONS.add("jeq");
    ONE_PARAM_INSTRUCTIONS.add("jne");
    ONE_PARAM_INSTRUCTIONS.add("jgt");
    ONE_PARAM_INSTRUCTIONS.add("call");
  }

  public static class ParsedInstructionData {
    long instrCode;
    public int instrSize = 1;
    boolean shortInstr = false;
    final List<Parameter> params = new LinkedList<>();
  }

  static class Parameter {
    int addTypeCode = -1;
    int regCode;
    int value;
    String symbol = "";
    int offset;
    boolean pcRel = false;
    boolean regHigh = false;
  }
  
  public ParsedInstructionData parse(String instr, int offset) throws SyntaxError {
    ParsedInstructionData data = new ParsedInstructionData();
    String instrName;

    instr = instr.trim();

    //instruction name ends with the first space
    int pos = indexOfFirstWhitespace(instr);
    if (pos >= 0) {
      instrName = instr.substring(0, pos);
      instr = instr.substring(pos).trim();
    } else instrName = instr;

    instrName = parseAndCutSizeSpecifier(data, instrName);

    if (INSTRUCTION_MAP.get(instrName) == null) throw new SyntaxError("Unknown Instruction: " + instrName);
    else data.instrCode = INSTRUCTION_MAP.get(instrName);

    processParams(instr, data, offset, instrName);

    return data;
  }

  public long createInstrCode(ParsedInstructionData data, SymbolTable symbolTable, List<EquSymbol> equTable,
                              List<RelocationSymbol> relocationTable) throws SyntaxError {
    String relocationSize = "16";
    if (data.shortInstr) relocationSize = "8";

    //first byte
    data.instrCode = data.instrCode << 1;
    if (!data.shortInstr) data.instrCode += 1;
    data.instrCode = data.instrCode << 2;

    for (int i = 0; i < data.params.size(); i++) {
      data.instrCode = data.instrCode << 3;
      data.instrCode += data.params.get(i).addTypeCode;
      addParamToInstrCode(data, symbolTable, equTable, relocationTable, relocationSize, i);
    }

    return data.instrCode;
  }

  private void addParamToInstrCode(ParsedInstructionData data, SymbolTable symbolTable, List<EquSymbol> equTable,
                                   List<RelocationSymbol> relocationTable, String relocationSize, int i) throws SyntaxError {
    boolean isSymbol = false;
    switch (data.params.get(i).addTypeCode) {
      case 0: //immediate
        data.instrCode = (data.instrCode << 5);
        isSymbol = ifSymbolResolveValue(data.params, symbolTable, equTable, relocationTable, relocationSize, i);
        addToInstrCode(data, isSymbol, i);
        break;
      case 1:
      case 2: //regdir & regind
        data.instrCode = (data.instrCode << 4);
        data.instrCode += data.params.get(i).regCode;
        data.instrCode = (data.instrCode << 1);
        if (data.params.get(i).regHigh) data.instrCode += 1;
        break;
      case 3:
      case 4: //reind8 & regind16
        data.instrCode = (data.instrCode << 4);
        data.instrCode += data.params.get(i).regCode;
        data.instrCode = (data.instrCode << 1);
        isSymbol = ifSymbolResolveValue(data.params, symbolTable, equTable, relocationTable, relocationSize, i);
        addToInstrCode(data, isSymbol, i);
        break;
      case 5: //mem
        data.instrCode = (data.instrCode << 5);
        if (data.params.get(i).symbol.length() > 0) {
          createRelocationEntry(data.params, symbolTable, relocationTable, relocationSize, i);
          isSymbol = true;
        }
        addToInstrCode(data, isSymbol, i);
    }
  }

  private void processParams(String instr, ParsedInstructionData data, int offset, String instrName) throws SyntaxError {

    if (!ZERO_PARAM_INSTRUCTIONS.contains(instrName)) {

      List<String> params = parameterParser.parse(instr);

      processParam(data, offset, params.get(0));
      updateInstrSize(data, 0);
      checkFirstParamValidity(data);

      if (!ONE_PARAM_INSTRUCTIONS.contains(instrName)) {
        processParam(data, offset, params.get(1));
        updateInstrSize(data, 1);
      }

    }
  }

  private void processParam(ParsedInstructionData data, int offset, String param)
      throws SyntaxError {
    if ("".equals(param)) throw new SyntaxError("Missing Instruction Parameter");

    Parameter paramStruct = parseParameter(param, offset + data.instrSize, data.shortInstr);
    data.params.add(paramStruct);
  }

  private void updateInstrSize(ParsedInstructionData data, int parameterIndex) {
    if (data.params.get(parameterIndex).addTypeCode == ADDRESSING_MAP.get("reg") ||
        data.params.get(parameterIndex).addTypeCode == ADDRESSING_MAP.get("regind")) data.instrSize++;
    else {
      if (data.shortInstr) data.instrSize += 2;
      else data.instrSize += 3;
    }
  }

  private void checkFirstParamValidity(ParsedInstructionData data) throws SyntaxError {
    if (data.params.get(0).addTypeCode == ADDRESSING_MAP.get("imm")
        && data.instrCode != INSTRUCTION_MAP.get("push")
        && data.instrCode != INSTRUCTION_MAP.get("int"))
      throw new SyntaxError("Destination Addressing Type can't be Immediate");
  }

  private String parseAndCutSizeSpecifier(ParsedInstructionData data, String instrName) {
    if (instrName.charAt(instrName.length() - 1) == 'w') {
      instrName = instrName.substring(0, instrName.length() - 1);
    } else if (instrName.charAt(instrName.length() - 1) == 'b' && !"sub".equals(instrName)) {
      data.shortInstr = true;
      instrName = instrName.substring(0, instrName.length() - 1);
    }
    return instrName;
  }

  private void addToInstrCode(ParsedInstructionData data, boolean isSymbol, int i) {
    if (data.shortInstr) {
      data.instrCode = (data.instrCode << 8);
      data.instrCode += (data.params.get(i).value %= (1 << 8));
    } else {
      data.instrCode = (data.instrCode << 16);
      data.params.get(i).value %= (1 << 16);
      if (!isSymbol) data.instrCode += Integer.parseInt(
          numberParser.swapBytes(
              numberParser.toHex(data.params.get(i).value, 2)), 16);
    }
  }

  private boolean ifSymbolResolveValue(List<Parameter> params, SymbolTable symbolTable, List<EquSymbol> equTable,
                                       List<RelocationSymbol> relocationTable, String relocSize,
                                       int paramIndex) throws SyntaxError {
    boolean isSymbol = false;
    if (params.get(paramIndex).symbol.length() > 0) {
      boolean found = false;
      for (EquSymbol sym : equTable)
        if (sym.getLabel().equals(params.get(paramIndex).symbol)) {
          params.get(paramIndex).value = sym.getValue();
          found = true;
          break;
        }
      if (!found) {
        isSymbol = true;
        createRelocationEntry(params, symbolTable, relocationTable, relocSize, paramIndex);
      }
    }
    return isSymbol;
  }

  private void createRelocationEntry(List<Parameter> params, SymbolTable symbolTable,
                                     List<RelocationSymbol> relocationTable, String relocSize,
                                     int paramIndex) throws SyntaxError {
    Symbol s;
    if ((s = symbolTable.find(params.get(paramIndex).symbol)) == null)
      throw new SyntaxError("Symbol not defined");
    //reloc type
    String relocType = "R_";
    if (params.get(paramIndex).pcRel) relocType += "PC";
    relocType += relocSize;
    relocationTable.add(new RelocationSymbol(params.get(paramIndex).offset, relocType, s.getSection(), s.getLabel()));
  }

  private Parameter parseParameter(String param, int offset, boolean shortInstr) throws SyntaxError {
    Parameter paramData = new Parameter();
    paramData.offset = offset + 1;

    char firstChar = param.charAt(0);

    if (firstChar == '&' || Character.isDigit(firstChar) || firstChar == '-') {
      return getImmediateParameter(param, paramData);
    }

    if (firstChar == '$') {
      return getPcRelParam(param, paramData, shortInstr);
    }

    if (firstChar == '*') {
      paramData.addTypeCode = ADDRESSING_MAP.get("mem");
      paramData.value = numberParser.parseInt(param.substring(1));
      return paramData;
    }

    if (firstChar == 'r' && Character.isDigit(param.charAt(1)) ||
        ((param.length() > 1 && ("pc".equals(param.substring(0, 2)) || "sp".equals(param.substring(0, 2))))
            && (param.length() == 2 || param.charAt(2) == '['))) {

      if (REGISTER_MAP.get(param.substring(0, 2)) == null)
        throw new SyntaxError("Unknown register: " + param.substring(0, 2));
      else paramData.regCode = REGISTER_MAP.get(param.substring(0, 2));

      if (getRegdirParam(param, paramData, shortInstr)) return paramData;
      return getRegindParam( param, paramData, shortInstr);
    }

    if (!Character.isDigit(firstChar)) {
      paramData.addTypeCode = ADDRESSING_MAP.get("mem");
      paramData.symbol = param;
      return paramData;
    }

    throw new SyntaxError("Bad Parameter");
  }

  private Parameter getRegindParam(String param, Parameter paramData, boolean shortInstr) throws SyntaxError {
    if (param.charAt(2) != '[') throw new SyntaxError("'[' expected");
    if (param.charAt(param.length() - 1) != ']') throw new SyntaxError("']' expected");
    param = param.substring(3, param.length() - 1);
    if (param.isEmpty()) {
      paramData.addTypeCode = ADDRESSING_MAP.get("regind");
      paramData.regHigh = false;
      return paramData;
    }
    if (Character.isDigit(param.charAt(0)) || param.charAt(0) == '-') paramData.value = numberParser.parseInt(param);
    else paramData.symbol = param;
    if (shortInstr) paramData.addTypeCode = ADDRESSING_MAP.get("regind8");
    else paramData.addTypeCode = ADDRESSING_MAP.get("regind16");
    return paramData;
  }

  private boolean getRegdirParam(String param, Parameter paramData, boolean shortInstr) throws SyntaxError {
    if (!shortInstr) {
      if (param.length() == 2) {
        paramData.addTypeCode = ADDRESSING_MAP.get("reg");
        return true;
      }
    } else {
      if (param.length() == 3) {
        switch (param.charAt(2)) {
          case 'l': paramData.regHigh = false; break;
          case 'h': paramData.regHigh = true; break;
          default: throw new SyntaxError("Expected 'h' or 'l' reg specifier");
        }
        paramData.addTypeCode = ADDRESSING_MAP.get("reg");
        return true;
      }
    }
    return false;
  }

  private Parameter getPcRelParam(String param, Parameter paramData, boolean shortInstr) {
    if (shortInstr) paramData.addTypeCode = ADDRESSING_MAP.get("regind8");
    else paramData.addTypeCode = ADDRESSING_MAP.get("regind16");
    paramData.pcRel = true;
    paramData.symbol = param.substring(1);
    return paramData;
  }

  private Parameter getImmediateParameter(String param, Parameter paramData) {
    paramData.addTypeCode = ADDRESSING_MAP.get("imm");
    if (param.charAt(0) == '&') paramData.symbol = param.substring(1);
    else paramData.value = numberParser.parseInt(param);
    return paramData;
  }

  private int indexOfFirstWhitespace(String str) {
    for(int i = 0; i < str.length(); i++) {
      if(Character.isWhitespace(str.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

}
