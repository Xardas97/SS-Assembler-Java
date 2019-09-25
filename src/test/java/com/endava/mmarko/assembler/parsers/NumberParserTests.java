package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.config.TwoPassAssemblerConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TwoPassAssemblerConfig.class)
public class NumberParserTests {
  @Autowired
  NumberParser numberParser;

  @Test(expected = IllegalArgumentException.class)
  public void toHexThrowsIllegalArgumentException() {
    numberParser.toHex(23, -1);
  }

  @Test
  public void toHexZero() {
    String ret = numberParser.toHex(0, 1);
    Assert.assertEquals("00", ret);
  }

  @Test
  public void toHexExactByteLength() {
    String ret = numberParser.toHex(17802, 2);
    Assert.assertEquals("458a", ret);
  }

  @Test
  public void toHexLowerByteLength() {
    String ret = numberParser.toHex(17802, 1);
    Assert.assertEquals("8a", ret);
  }

  @Test
  public void toHexHigherByteLength() {
    String ret = numberParser.toHex(17802, 4);
    Assert.assertEquals("0000458a", ret);
  }

  @Test
  public void toHexNegativeNumber() {
    String ret = numberParser.toHex(-5, 8);
    Assert.assertEquals("fffffffffffffffb", ret);
  }

  @Test(expected = NullPointerException.class)
  public void formatThrowsNullPointerException() {
    numberParser.format(null);
  }

  @Test
  public void formatAddsSpaces() {
    String ret = numberParser.format("AB8FD5E6A9F");
    Assert.assertEquals("AB 8F D5 E6 A9 F", ret);
  }

  @Test(expected = NullPointerException.class)
  public void swapBytesThrowsNullPointerException() {
    numberParser.swapBytes(null);
  }

  @Test
  public void swapBytesSuccessfullySwaps() {
    String ret = numberParser.swapBytes("F83B");
    Assert.assertEquals("3BF8", ret);
  }

  @Test
  public void swapBytesDoesNothingForLessThanFour() {
    String ret = numberParser.swapBytes("FA3");
    Assert.assertEquals("FA3", ret);
  }

  @Test
  public void swapBytesDoesNothingForMoreThanFour() {
    String ret = numberParser.swapBytes("FA3B8");
    Assert.assertEquals("FA3B8", ret);
  }

  @Test
  public void swapBytesDoesNothingForZeroLength() {
    String ret = numberParser.swapBytes("");
    Assert.assertEquals("", ret);
  }

  @Test(expected = NullPointerException.class)
  public void parseIntThrowsNullPointerException() {
    numberParser.parseInt(null);
  }

  @Test
  public void parseIntParsesDecimalZero() {
    int ret = numberParser.parseInt("0");
    Assert.assertEquals(0, ret);
  }

  @Test
  public void parseIntParsesDecimalPositive() {
    int ret = numberParser.parseInt("45123656");
    Assert.assertEquals(45123656, ret);
  }

  @Test
  public void parseIntParsesDecimalNegative() {
    int ret = numberParser.parseInt("-5542121");
    Assert.assertEquals(-5542121, ret);
  }

  @Test
  public void parseIntParseHexadecimalZero() {
    int ret = numberParser.parseInt("0x0");
    Assert.assertEquals(0, ret);
  }

  @Test
  public void parseIntParsesHexadecimalPositive() {
    int ret = numberParser.parseInt("0x7F4B");
    Assert.assertEquals(32587, ret);
  }

  @Test
  public void parseIntParseOctalZero() {
    int ret = numberParser.parseInt("00");
    Assert.assertEquals(0, ret);
  }

  @Test
  public void parseIntParsesOctalPositive() {
    int ret = numberParser.parseInt("024615657");
    Assert.assertEquals(5446575, ret);
  }

  @Test
  public void parseIntParseByteZero() {
    int ret = numberParser.parseInt("0b0");
    Assert.assertEquals(0, ret);
  }

  @Test
  public void parseIntParsesBytePositive() {
    int ret = numberParser.parseInt("0b11010000001100110001001011");
    Assert.assertEquals(54578251, ret);
  }

  @Test(expected = NumberFormatException.class)
  public void parseIntThrowsNumberFormatException() {
    numberParser.parseInt("");
  }

  @Test
  public void parseIntParsesMultipleZeroes() {
    int ret = numberParser.parseInt("000000");
    Assert.assertEquals(0, ret);
  }

}
