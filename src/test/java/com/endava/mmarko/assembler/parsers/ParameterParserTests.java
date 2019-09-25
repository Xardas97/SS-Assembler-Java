package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SyntaxError;
import com.endava.mmarko.assembler.config.TwoPassAssemblerConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TwoPassAssemblerConfig.class)
public class ParameterParserTests {
  @Autowired
  ParameterParser parameterParser;

  @Test
  public void parseParsesOneParamSuccessfully() throws SyntaxError {
    List<String> ret = parameterParser.parse("  p1   ");
    List<String> expected = Collections.singletonList("p1");
    Assert.assertEquals(expected, ret);
  }

  @Test
  public void parseParsesMultipleParamsSuccessfully() throws SyntaxError {
    List<String> ret = parameterParser.parse("  p1  ,   p2  , p3    ");
    List<String> expected = Arrays.asList("p1", "p2", "p3");
    Assert.assertEquals(expected, ret);
  }

  @Test(expected = NullPointerException.class)
  public void parseThrowsNullPointerException() throws SyntaxError {
    parameterParser.parse(null);
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorForNoParams() throws SyntaxError {
    parameterParser.parse("    ");
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorFoMissingMiddleParam() throws SyntaxError {
    parameterParser.parse("param1,  , param2");
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorFoMissingMiddleParamNoSpace() throws SyntaxError {
    parameterParser.parse("param1,,param2");
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorForMultipleMissingParams() throws SyntaxError {
    parameterParser.parse(",,param2");
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorForNoParamsWhereMultipleIsExpected() throws SyntaxError {
    parameterParser.parse(",,");
  }

  @Test(expected = SyntaxError.class)
  public void parseThrowsSyntaxErrorFoMissingLastParam() throws SyntaxError {
    parameterParser.parse("param1, Param2,  ");
  }

}
