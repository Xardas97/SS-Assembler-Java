package com.endava.mmarko.assembler;

public class SectionLocation {
  private int id;
  private int offset;

  public SectionLocation() {
    this.id = -1;
    this.offset = 0;
  }

  boolean isInvalid() {
    return id < 0;
  }

  boolean isValid() {
    return !isInvalid();
  }

   void incrementSection() {
    id++;
    offset = 0;
  }

  void incrementOffset() {
    offset++;
  }

  void incrementOffset(int increment) {
    offset += increment;
  }

  public int getId() {
    return id;
  }

  public int getOffset() {
    return offset;
  }
}
