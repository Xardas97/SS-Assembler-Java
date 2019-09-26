package com.endava.mmarko.assembler;

import com.endava.mmarko.assembler.config.TwoPassAssemblerConfig;
import java.io.IOException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class App {
  public static void main(String[] args) {
    if (args.length != 3 || !"-o".equals(args[0])) {
      System.out.println("bad arguments");
      return;
    }

    String inputFile = args[2];
    String outputFile = args[1];

    ApplicationContext context = new AnnotationConfigApplicationContext(TwoPassAssemblerConfig.class);
    Assembler as = context.getBean(Assembler.class);

    try {
      as.assemble(inputFile, outputFile);
    } catch (IOException e) {
      System.out.println("Bad Files");
    } catch (SyntaxError e) {
      System.out.println("SYNTAX ERROR in line: " + e.getLine() + "\n");
      System.out.println(e.getMessage() + "\n");
    }
  }
}
