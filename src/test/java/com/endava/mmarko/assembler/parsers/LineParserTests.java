package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SyntaxError;
import com.endava.mmarko.assembler.config.TwoPassAssemblerConfig;
import com.endava.mmarko.assembler.tables.EquSymbol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.endava.mmarko.assembler.parsers.LineParser.ParsedLineData;
import com.endava.mmarko.assembler.parsers.LineParser.LineType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TwoPassAssemblerConfig.class)
public class LineParserTests {
  @Mock
  NumberParser numberParser;
  @Mock
  ParameterParser parameterParser;
  @InjectMocks
  LineParser lineParser;

  @Test(expected = NullPointerException.class)
  public void parseParsesDirectiveThrowsNullPointerExceptionForEquTable() throws SyntaxError {
    List<String> params = Collections.singletonList("5");
    Mockito.when(parameterParser.parse("   5")).thenReturn(params);
    Mockito.when(numberParser.parseInt("5")).thenReturn(5);

    lineParser.parse("  .word   5", null);
  }

  @Test
  public void parseParsesDirectiveWord() throws SyntaxError {
    List<EquSymbol> equTable = Arrays.asList(
        new EquSymbol("main", 12),
        new EquSymbol("other", 15));
    List<Integer> expectedValues = Arrays.asList(12, 3, 15);

    List<String> params = Arrays.asList("main", "3", "other");
    Mockito.when(parameterParser.parse("   main, 3 , other")).thenReturn(params);
    Mockito.when(numberParser.parseInt("main")).thenThrow(new NumberFormatException());
    Mockito.when(numberParser.parseInt("3")).thenReturn(3);
    Mockito.when(numberParser.parseInt("other")).thenThrow(new NumberFormatException());

    ParsedLineData data = lineParser.parse("  .word   main, 3 , other ", equTable);

    Assert.assertEquals("word", data.directiveName);
    Assert.assertEquals(LineType.DIRECTIVE, data.type);
    Assert.assertEquals(expectedValues, data.values);
  }

  @Test
  public void parseParsesDirectiveEqu() throws SyntaxError {
    List<Integer> expectedValues = Collections.singletonList(5);

    List<String> params = Arrays.asList("main", "5");
    Mockito.when(parameterParser.parse("   main , 5")).thenReturn(params);
    Mockito.when(numberParser.parseInt("5")).thenReturn(5);

    ParsedLineData data = lineParser.parse("  .equ   main , 5 ", null);

    Assert.assertEquals("equ", data.directiveName);
    Assert.assertEquals(LineType.DIRECTIVE, data.type);
    Assert.assertEquals(expectedValues, data.values);
    Assert.assertEquals("main", data.symbol);
  }

  @Test
  public void parseParsesDirectiveExtern() throws SyntaxError {
    List<String> params = Collections.singletonList("main");
    Mockito.when(parameterParser.parse("   main")).thenReturn(params);

    ParsedLineData data = lineParser.parse("  .extern   main  ", null);

    Assert.assertEquals("extern", data.directiveName);
    Assert.assertEquals(LineType.DIRECTIVE, data.type);
    Assert.assertTrue(data.values.isEmpty());
    Assert.assertEquals("main", data.symbol);
  }

  @Test
  public void parseParsesDirectiveSkip() throws SyntaxError {
    List<Integer> expectedValues = Collections.singletonList(3);

    List<String> params = Arrays.asList("3", "5", "4");
    Mockito.when(parameterParser.parse("  3, 5, 4")).thenReturn(params);
    Mockito.when((numberParser.parseInt("3"))).thenReturn(3);

    ParsedLineData data = lineParser.parse("  .skip  3, 5, 4  ", null);

    Assert.assertEquals("", data.label);
    Assert.assertEquals("", data.sectionName);
    Assert.assertEquals("skip", data.directiveName);
    Assert.assertEquals("", data.instruction);

    Assert.assertEquals(LineType.DIRECTIVE, data.type);

    Assert.assertEquals(expectedValues, data.values);
    Assert.assertNull(data.symbol);
    Assert.assertNull(data.sectionFlags);
  }

  @Test(expected = SyntaxError.class)
  public void parseParsesSectionThrowsSyntaxErrorSectionNameTooLong() throws SyntaxError {
    List<String> params = Arrays.asList("sectionsectionsectionsec", "rwx");
    Mockito.when(parameterParser.parse("   sectionsectionsectionsec,  rwx")).thenReturn(params);
    lineParser.parse("  .section   sectionsectionsectionsec,  rwx  ", null);
  }

  @Test(expected = SyntaxError.class)
  public void parseParsesSectionThrowsSyntaxErrorNotEnoughParameters() throws SyntaxError {
    List<String> params = Collections.singletonList("sec");
    Mockito.when(parameterParser.parse("   sec")).thenReturn(params);

    lineParser.parse("  .section   sec  ", null);
  }

  @Test(expected = SyntaxError.class)
  public void parseParsesSectionThrowsSyntaxErrorTooManyParameters() throws SyntaxError {
    List<String> params = Arrays.asList("sec", "rw", "x");
    Mockito.when(parameterParser.parse("   sec, rw, x")).thenReturn(params);

    lineParser.parse("  .section   sec, rw, x    ", null);
  }

  @Test
  public void parseParsesSectionSection() throws SyntaxError {
    List<String> params = Arrays.asList("sec", "rwx");
    Mockito.when(parameterParser.parse("   sec,  rwx")).thenReturn(params);

    ParsedLineData data = lineParser.parse("  .section   sec,  rwx  ", null);

    Assert.assertEquals("", data.label);
    Assert.assertEquals("sec", data.sectionName);
    Assert.assertEquals("", data.directiveName);
    Assert.assertEquals("", data.instruction);

    Assert.assertEquals(LineType.SECTION, data.type);

    Assert.assertTrue(data.values.isEmpty());
    Assert.assertNull(data.symbol);
    Assert.assertEquals("rwx", data.sectionFlags);
  }

  @Test
  public void parseParsesTextSection() throws SyntaxError {
    ParsedLineData data = lineParser.parse("  .text ", null);

    Assert.assertEquals("", data.label);
    Assert.assertEquals("text", data.sectionName);
    Assert.assertEquals("", data.directiveName);
    Assert.assertEquals("", data.instruction);

    Assert.assertEquals(LineType.SECTION, data.type);

    Assert.assertTrue(data.values.isEmpty());
    Assert.assertNull(data.symbol);
    Assert.assertEquals("rx", data.sectionFlags);
  }

  @Test
  public void parseParsesDataSection() throws SyntaxError {
    ParsedLineData data = lineParser.parse("  .data ", null);

    Assert.assertEquals("data", data.sectionName);
    Assert.assertEquals(LineType.SECTION, data.type);
    Assert.assertEquals("rw", data.sectionFlags);
  }

  @Test
  public void parseParsesBssSection() throws SyntaxError {
    ParsedLineData data = lineParser.parse("  .bss ", null);

    Assert.assertEquals("bss", data.sectionName);
    Assert.assertEquals(LineType.SECTION, data.type);
    Assert.assertEquals("r", data.sectionFlags);
  }

  @Test
  public void parseParsesComment() throws SyntaxError {
    ParsedLineData data = lineParser.parse("   lab   :   ;mov r3, *14   \t  ", null);

    Assert.assertEquals("lab", data.label);
    Assert.assertEquals("", data.sectionName);
    Assert.assertEquals("", data.directiveName);
    Assert.assertEquals("", data.instruction);

    Assert.assertEquals(LineType.EMPTY, data.type);

    Assert.assertTrue(data.values.isEmpty());
    Assert.assertNull(data.symbol);
    Assert.assertNull(data.sectionFlags);
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorForUnknownSection() throws SyntaxError {
    lineParser.parse("   .dir  ", null);
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorForUnexpectedDot() throws SyntaxError {
    lineParser.parse("   d.text  ", null);
  }

  @Test
  public void parseParsesLabelAndInstruction() throws SyntaxError {
    ParsedLineData data = lineParser.parse("   lab   :   mov r3, *14   \t  ", null);

    Assert.assertEquals("lab", data.label);
    Assert.assertEquals("", data.sectionName);
    Assert.assertEquals("", data.directiveName);
    Assert.assertEquals("mov r3, *14", data.instruction);

    Assert.assertEquals(LineType.INSTRUCTION, data.type);

    Assert.assertTrue(data.values.isEmpty());
    Assert.assertNull(data.symbol);
    Assert.assertNull(data.sectionFlags);
  }

  @Test(expected = NullPointerException.class)
  public void parseThrowsNullPointerException() throws SyntaxError {
    lineParser.parse(null, null);
  }

  @Test
  public void parseParsesLabelSuccessfully() throws SyntaxError {
    ParsedLineData data = lineParser.parse("   bla:    ", null);

    Assert.assertEquals("bla", data.label);
    Assert.assertEquals("", data.sectionName);
    Assert.assertEquals("", data.directiveName);
    Assert.assertEquals("", data.instruction);

    Assert.assertEquals(LineType.EMPTY, data.type);

    Assert.assertTrue(data.values.isEmpty());
    Assert.assertNull(data.symbol);
    Assert.assertNull(data.sectionFlags);
  }

  @Test(expected = SyntaxError.class)
  public void parseParsesLabelWithWhitespaces() throws SyntaxError {
    lineParser.parse("   bl a:    ", null);
  }

  @Test(expected = SyntaxError.class)
  public void parseParsesLabelWithNothingAfterIt() throws SyntaxError {
    lineParser.parse("   bl a:", null);
  }

  @Test(expected = SyntaxError.class)
  public void parseParsesEmptyLabel() throws SyntaxError {
    lineParser.parse("   :    ", null);
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorForUnexpectedColon() throws SyntaxError {
    lineParser.parse("   bla1:   bla2: instr", null);
  }

  @Test
  public void parseParsesEmptyLine() throws SyntaxError {
    ParsedLineData data = lineParser.parse("       ", null);

    Assert.assertEquals("", data.label);
    Assert.assertEquals("", data.sectionName);
    Assert.assertEquals("", data.directiveName);
    Assert.assertEquals("", data.instruction);

    Assert.assertEquals(LineType.EMPTY, data.type);

    Assert.assertTrue(data.values.isEmpty());
    Assert.assertNull(data.symbol);
    Assert.assertNull(data.sectionFlags);
  }

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }
}
