package com.endava.mmarko.assembler;

public class SyntaxError extends Exception {
  private String line;

  public SyntaxError(String message) {
    super(message);
  }

  public String getLine() {
    return line;
  }

  public void setLine(String line) {
    this.line = line;
  }
}
