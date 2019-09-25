package com.endava.mmarko.assembler.parsers;

import org.springframework.stereotype.Component;

@Component
public class NumberParser {

  int parseInt(String str) {
    if (str.length() > 2 && "0x".equals(str.substring(0, 2))) {
      return Integer.parseInt(str.substring(2), 16);
    }
    if(str.length() > 2 && "0b".equals(str.substring(0, 2))) {
      return Integer.parseInt(str.substring(2), 2);
    }
    if(str.length() > 1 && "0".equals(str.substring(0, 1))) {
      return Integer.parseInt(str.substring(1), 8);
    }
    return Integer.parseInt(str);
  }

  public String swapBytes(String str) {
    if (str.length() != 4) {
      return str;
    }
    return str.substring(2) + str.substring(0, 2);
  }

  public String format(String str) {
    StringBuilder result = new StringBuilder();
    int breakPoint = 68;

    for (int i = 0; i < str.length(); i++, breakPoint--) {
      result.append(str.charAt(i));
      if (i % 2 == 1) {
        if (breakPoint < 0) {
          result.append("\n");
          breakPoint = 68;
        }
        else {
          result.append(" ");
        }
      }
    }

    return result.toString();
  }

  public String toHex(long value, int byteSize) {
    return NumberParser.toHexStatic(value, byteSize);
  }

  public static String toHexStatic(long value, int byteSize) {
    if (byteSize < 0) throw new IllegalArgumentException();
    value = value % (long) Math.pow(2, byteSize * 8);

    StringBuilder result = new StringBuilder(Long.toHexString(value));

    int missingChars = byteSize * 2 - result.length();
    result.insert(0, "0".repeat(missingChars));

    return result.toString();
  }

}
