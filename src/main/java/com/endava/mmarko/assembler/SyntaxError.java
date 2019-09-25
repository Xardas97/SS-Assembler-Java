package com.endava.mmarko.assembler;

public class SyntaxError extends Exception {
  private String line;

  public SyntaxError(String message) {
    super(message);
  }

  String getLine() {
    return line;
  }

  void setLine(String line) {
    this.line = line;
  }
}
