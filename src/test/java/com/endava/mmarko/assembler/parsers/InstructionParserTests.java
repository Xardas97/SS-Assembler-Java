package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SectionLocation;
import com.endava.mmarko.assembler.SyntaxError;
import com.endava.mmarko.assembler.config.TwoPassAssemblerConfig;
import com.endava.mmarko.assembler.tables.EquSymbol;
import com.endava.mmarko.assembler.tables.RelocationSymbol;
import com.endava.mmarko.assembler.tables.Symbol;
import com.endava.mmarko.assembler.tables.SymbolTable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TwoPassAssemblerConfig.class)
public class InstructionParserTests {
  @Mock
  NumberParser numberParser;
  @Mock
  ParameterParser parameterParser;
  @InjectMocks
  InstructionParser instructionParser;

  @Test
  public void createInstructionCodeWithMemParamSuccessfully() throws SyntaxError {
    Mockito.when(numberParser.toHex(Mockito.anyLong(), Mockito.anyInt())).thenReturn("F");
    Mockito.when(numberParser.swapBytes(Mockito.anyString())).thenReturn("F");

    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    SymbolTable symbolTable = new SymbolTable();
    List<RelocationSymbol> relocationTable = new LinkedList<>();
    InstructionParser.Parameter param = new InstructionParser.Parameter();

    param.addTypeCode = 5;
    param.symbol = "sym";
    symbolTable.add("sym", new SectionLocation());
    data.params.add(param);

    instructionParser.createInstrCode(data, symbolTable, null, relocationTable);
  }

  @Test
  public void createInstructionCodeWithImmediateSymbolParamSuccessfully() throws SyntaxError {
    Mockito.when(numberParser.toHex(Mockito.anyLong(), Mockito.anyInt())).thenReturn("F");
    Mockito.when(numberParser.swapBytes(Mockito.anyString())).thenReturn("F");

    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    List<EquSymbol> equTable = Collections.singletonList(new EquSymbol("sym", 3));
    InstructionParser.Parameter param = new InstructionParser.Parameter();

    param.addTypeCode = 0;
    param.symbol = "sym";
    data.params.add(param);

    instructionParser.createInstrCode(data, null, equTable, null);
  }

  @Test
  public void createInstructionCodeExecutedSuccessfully() throws SyntaxError {
    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    instructionParser.createInstrCode(data, null, null, null);
  }

  @Test
  public void createInstructionCodeWithRegParamSuccessfully() throws SyntaxError {
    Mockito.when(numberParser.toHex(Mockito.anyLong(), Mockito.anyInt())).thenReturn("F");
    Mockito.when(numberParser.swapBytes(Mockito.anyString())).thenReturn("F");

    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    InstructionParser.Parameter param = new InstructionParser.Parameter();

    param.addTypeCode = 1;
    data.params.add(param);

    instructionParser.createInstrCode(data, null, null, null);
  }

  @Test
  public void createInstructionCodeWithRegindParamSuccessfully() throws SyntaxError {
    Mockito.when(numberParser.toHex(Mockito.anyLong(), Mockito.anyInt())).thenReturn("F");
    Mockito.when(numberParser.swapBytes(Mockito.anyString())).thenReturn("F");

    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    InstructionParser.Parameter param = new InstructionParser.Parameter();

    param.addTypeCode = 3;
    data.params.add(param);

    instructionParser.createInstrCode(data, null, null, null);
  }

  @Test
  public void createInstructionCodeWithParamSuccessfully() throws SyntaxError {
    Mockito.when(numberParser.toHex(Mockito.anyLong(), Mockito.anyInt())).thenReturn("F");
    Mockito.when(numberParser.swapBytes(Mockito.anyString())).thenReturn("F");

    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    InstructionParser.Parameter param = new InstructionParser.Parameter();

    param.addTypeCode = 0;
    data.params.add(param);

    instructionParser.createInstrCode(data, null, null, null);
  }

  @Test
  public void createInstructionCodeWithShortInstructionParamSuccessfully() throws SyntaxError {
    Mockito.when(numberParser.toHex(Mockito.anyLong(), Mockito.anyInt())).thenReturn("F");
    Mockito.when(numberParser.swapBytes(Mockito.anyString())).thenReturn("F");

    InstructionParser.ParsedInstructionData data = new InstructionParser.ParsedInstructionData();
    InstructionParser.Parameter param = new InstructionParser.Parameter();

    param.addTypeCode = 0;
    data.params.add(param);
    data.shortInstr = true;

    instructionParser.createInstrCode(data, null, null, null);
  }

  @Test
  public void parseExecutesSuccessfully() throws SyntaxError {
    List<String> params = Arrays.asList("*3", "-0x8");
    Mockito.when(parameterParser.parse("*3, -0x8")).thenReturn(params);
    instructionParser.parse("mov *3, -0x8", 0);
  }

  @Test
  public void parseParsesRegdirWSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("r3")).thenReturn(Collections.singletonList("r3"));
    instructionParser.parse("pushw r3", 0);
  }

  @Test
  public void parseParsesRegdirWithSpecifierWSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("r3l")).thenReturn(Collections.singletonList("r3l"));
    instructionParser.parse("pushb r3l", 0);
  }

  @Test
  public void parseParsesPcRelWSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("$3")).thenReturn(Collections.singletonList("$3"));
    instructionParser.parse("push $3", 0);
  }

  @Test
  public void parseParsesMemSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("memloc")).thenReturn(Collections.singletonList("memloc"));
    instructionParser.parse("push memloc", 0);
  }

  @Test
  public void parseParsesRegindWSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("r3[]")).thenReturn(Collections.singletonList("r3[]"));
    instructionParser.parse("pushw r3[]", 0);
  }

  @Test
  public void parseParsesRegindPomWSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("r3[3]")).thenReturn(Collections.singletonList("r3[3]"));
    instructionParser.parse("pushw r3[3]", 0);
  }

  @Test
  public void parseParsesSizeSpecifierWSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("*3")).thenReturn(Collections.singletonList("*3"));
    instructionParser.parse("pushw *3", 0);
  }

  @Test
  public void parseParsesSizeSpecifierBSuccessfully() throws SyntaxError {
    Mockito.when(parameterParser.parse("*3")).thenReturn(Collections.singletonList("*3"));
    instructionParser.parse("pushb *3", 0);
  }

  @Test
  public void parseParsesNoParamInstructionSuccessfully() throws SyntaxError {
    instructionParser.parse("halt", 0);
  }

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }
}
