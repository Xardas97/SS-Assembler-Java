package com.endava.mmarko;

import java.io.IOException;
import java.util.List;

interface Assembler extends AutoCloseable {
  default void assemble() throws IOException {
      List<String> firstPassOutput = firstPass();
      secondPass(firstPassOutput);
  }

  List<String> firstPass() throws IOException;

  void secondPass(List<String> firstPassOutput) throws IOException;

  @Override
  void close() throws IOException;
}
