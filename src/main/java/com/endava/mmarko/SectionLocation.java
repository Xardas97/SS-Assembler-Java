package com.endava.mmarko;

class SectionLocation {
  private int id;
  private int offset;

  SectionLocation() {
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

  int getId() {
    return id;
  }

  int getOffset() {
    return offset;
  }
}
