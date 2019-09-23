package com.endava.mmarko;

import java.util.*;

class InstructionParser {
  private static final Map<String, Integer> INSTRUCTION_MAP;
  private static final Map<String, Integer> ADDRESSING_MAP;
  private static final Map<String, Integer> REGISTER_MAP;
  private static final List<String> ZERO_PARAM_INSTRUCTIONS;
  private static final List<String> ONE_PARAM_INSTRUCTIONS;

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

  private long instrCode;
  private int instrSize;
  private boolean shortInstr;
  private List<Parameter> params;

  static class Parameter {
    int addTypeCode = -1;
    int regCode;
    int value;
    String symbol = "";
    int offset;
    boolean pcRel = false;
    boolean regHigh = false;
  }

  InstructionParser(String instr, int offset) throws SyntaxError {
    String instrName;
    params = new LinkedList<>();
    instrSize = 1;
    shortInstr = false;

    instr = instr.trim();

    //instruction name ends with the first space
    int pos = indexOfFirstWhitespace(instr);
    if (pos >= 0) {
      instrName = instr.substring(0, pos);
      instr = instr.substring(pos).trim();
    } else instrName = instr;

    instrName = parseAndCutSizeSpecifier(instrName);

    if (INSTRUCTION_MAP.get(instrName) == null) throw new SyntaxError("Unknown Instruction: " + instrName);
    else instrCode = INSTRUCTION_MAP.get(instrName);

    if (!ZERO_PARAM_INSTRUCTIONS.contains(instrName)) {
      ParameterHelper parameterHelper = new ParameterHelper(instr);
      processParam(offset, parameterHelper,0);
      if (!ONE_PARAM_INSTRUCTIONS.contains(instrName)) {
        processParam(offset, parameterHelper,1);
      }
    }
  }

  long createInstrCode(SymbolTable symbolTable, List<EquSymbol> equTable,
                       List<RelocationSymbol> relocationTable) throws SyntaxError {
    boolean isSymbol = false;
    String relocationSize = "16";
    if (shortInstr) relocationSize = "8";

    //first byte
    instrCode = instrCode << 1;
    if (!shortInstr) instrCode += 1;
    instrCode = instrCode << 2;

    for (int i = 0; i < params.size(); i++) {
      instrCode = (instrCode << 3);
      instrCode += params.get(i).addTypeCode;
      switch (params.get(i).addTypeCode) {
        case 0: //immediate
          instrCode = (instrCode << 5);
          isSymbol = ifSymbolResolveValue(symbolTable, equTable, relocationTable, relocationSize, i);
          addToInstrCode(isSymbol, i);
          break;
        case 1:
        case 2: //regdir & regind
          instrCode = (instrCode << 4);
          instrCode += params.get(i).regCode;
          instrCode = (instrCode << 1);
          if (params.get(i).regHigh) instrCode += 1;
          break;
        case 3:
        case 4: //reind8 & regind16
          instrCode = (instrCode << 4);
          instrCode += params.get(i).regCode;
          instrCode = (instrCode << 1);
          isSymbol = ifSymbolResolveValue(symbolTable, equTable, relocationTable, relocationSize, i);
          addToInstrCode(isSymbol, i);
          break;
        case 5: //mem
          instrCode = (instrCode << 5);
          if (params.get(i).symbol.length() > 0) {
            createRelocationEntry(symbolTable, relocationTable, relocationSize, i);
            isSymbol = true;
          }
          addToInstrCode(isSymbol, i);
      }
    }
    return instrCode;
  }

  private int indexOfFirstWhitespace(String str) {
    for(int i = 0; i < str.length(); i++) {
      if(Character.isWhitespace(str.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private void processParam(int offset, ParameterHelper parameterHelper, int paramIndex) throws SyntaxError {
    String param = parameterHelper.getParam(paramIndex);
    if ("".equals(param)) throw new SyntaxError("Missing Instruction Parameter");

    params.add(parseParameter(param, offset + instrSize));
    if (paramIndex == 0) checkFirstParamValidity();

    updateInstrSize(paramIndex);
  }

  private void updateInstrSize(int parameterIndex) {
    if (params.get(parameterIndex).addTypeCode == ADDRESSING_MAP.get("reg") ||
        params.get(parameterIndex).addTypeCode == ADDRESSING_MAP.get("regind")) instrSize++;
    else {
      if (shortInstr) instrSize += 2;
      else instrSize += 3;
    }
  }

  private void checkFirstParamValidity() throws SyntaxError {
    if (params.get(0).addTypeCode == ADDRESSING_MAP.get("imm")
        && instrCode != INSTRUCTION_MAP.get("push")
        && instrCode != INSTRUCTION_MAP.get("int"))
      throw new SyntaxError("Destination Addressing Type can't be Immediate");
  }

  private String parseAndCutSizeSpecifier(String instrName) {
    if (instrName.charAt(instrName.length() - 1) == 'w') {
      instrName = instrName.substring(0, instrName.length() - 1);
    } else if (instrName.charAt(instrName.length() - 1) == 'b' && !"sub".equals(instrName)) {
      shortInstr = true;
      instrName = instrName.substring(0, instrName.length() - 1);
    }
    return instrName;
  }

  private void addToInstrCode(boolean isSymbol, int i) {
    if (shortInstr) {
      instrCode = (instrCode << 8);
      instrCode += (params.get(i).value %= (1 << 8));
    } else {
      instrCode = (instrCode << 16);
      params.get(i).value %= (1 << 16);
      if (!isSymbol) instrCode += Integer.parseInt(
          NumberParser.swapBytes(
              NumberParser.toHex(params.get(i).value, 2)), 16);
    }
  }

  private boolean ifSymbolResolveValue(SymbolTable symbolTable, List<EquSymbol> equTable,
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
        createRelocationEntry(symbolTable, relocationTable, relocSize, paramIndex);
      }
    }
    return isSymbol;
  }

  private void createRelocationEntry(SymbolTable symbolTable,
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

  private Parameter parseParameter(String param, int offset) throws SyntaxError {
    Parameter paramStruct = new Parameter();
    paramStruct.offset = offset + 1;

    char firstChar = param.charAt(0);

    if (firstChar == '&' || Character.isDigit(firstChar) || firstChar == '-') {
      return getImmediateParameter(param, paramStruct);
    }

    if (firstChar == '$') {
      return getRegindParameter(param, paramStruct);
    }

    if (firstChar == '*') {
      paramStruct.addTypeCode = ADDRESSING_MAP.get("mem");
      paramStruct.value = NumberParser.parseInt(param.substring(1));
      return paramStruct;
    }

    if (firstChar == 'r' && Character.isDigit(param.charAt(1)) ||
        ((param.length() > 1 && ("pc".equals(param.substring(0, 2)) || "sp".equals(param.substring(0, 2))))
            && (param.length() == 2 || param.charAt(2) == '['))) {

      if (REGISTER_MAP.get(param.substring(0, 2)) == null)
        throw new SyntaxError("Unknown register: " + param.substring(0, 2));
      else paramStruct.regCode = REGISTER_MAP.get(param.substring(0, 2));

      if (getRegdirParam(param, paramStruct)) return paramStruct;
      return getRegindParam(param, paramStruct);
    }

    if (!Character.isDigit(firstChar)) {
      paramStruct.addTypeCode = ADDRESSING_MAP.get("mem");
      paramStruct.symbol = param;
      return paramStruct;
    }

    throw new SyntaxError("Bad Parameter");
  }

  private Parameter getRegindParam(String param, Parameter paramStruct) throws SyntaxError {
    if (param.charAt(2) != '[') throw new SyntaxError("'[' expected");
    if (param.charAt(param.length() - 1) != ']') throw new SyntaxError("']' expected");
    param = param.substring(3, param.length() - 1);
    if (param.isEmpty()) {
      paramStruct.addTypeCode = ADDRESSING_MAP.get("regind");
      paramStruct.regHigh = false;
      return paramStruct;
    }
    if (Character.isDigit(param.charAt(0)) || param.charAt(0) == '-') paramStruct.value = NumberParser.parseInt(param);
    else paramStruct.symbol = param;
    if (shortInstr) paramStruct.addTypeCode = ADDRESSING_MAP.get("regind8");
    else paramStruct.addTypeCode = ADDRESSING_MAP.get("regind16");
    return paramStruct;
  }

  private boolean getRegdirParam(String param, Parameter paramStruct) throws SyntaxError {
    if (!shortInstr) {
      if (param.length() == 2) {
        paramStruct.addTypeCode = ADDRESSING_MAP.get("reg");
        return true;
      }
    } else {
      if (param.length() == 3) {
        switch (param.charAt(2)) {
          case 'l': paramStruct.regHigh = false; break;
          case 'h': paramStruct.regHigh = true; break;
          default: throw new SyntaxError("Expected 'h' or 'l' reg specifier");
        }
        paramStruct.addTypeCode = ADDRESSING_MAP.get("reg");
        return true;
      }
    }
    return false;
  }

  private Parameter getRegindParameter(String param, Parameter paramStruct) {
    if (shortInstr) paramStruct.addTypeCode = ADDRESSING_MAP.get("regind8");
    else paramStruct.addTypeCode = ADDRESSING_MAP.get("regind16");
    paramStruct.pcRel = true;
    paramStruct.symbol = param.substring(1);
    return paramStruct;
  }

  private Parameter getImmediateParameter(String param, Parameter paramStruct) {
    paramStruct.addTypeCode = ADDRESSING_MAP.get("imm");
    if (param.charAt(0) == '&') paramStruct.symbol = param.substring(1);
    else paramStruct.value = NumberParser.parseInt(param);
    return paramStruct;
  }

  int getInstrSize() {
    return instrSize;
  }

}
