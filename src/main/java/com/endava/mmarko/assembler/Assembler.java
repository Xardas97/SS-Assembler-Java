package com.endava.mmarko.assembler;

import java.io.IOException;
import java.util.List;

public interface Assembler {
  void assemble(String inputFileName, String outputFileName) throws IOException, SyntaxError;

  List<String> firstPass(String inputFileNam0e, AssemblerImpl.Tables tables) throws IOException, SyntaxError;

  void secondPass(List<String> firstPassOutput, String outputFileName,
                  AssemblerImpl.Tables tables) throws IOException, SyntaxError;
}
