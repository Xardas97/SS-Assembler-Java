package com.endava.mmarko;

import java.io.IOException;

public class App {
  public static void main(String[] args) {
    if (args.length != 3 || !"-o".equals(args[0])) {
      System.out.println("bad arguments");
      return;
    }

   try(Assembler as = new AssemblerImpl(args[2], args[1])) {
      as.assemble();
    } catch (IOException e) {
      System.out.println("Bad Files");
    }
  }
}
