package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SyntaxError;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
class ParameterParser {
  List<String> parse(String line) throws SyntaxError {
    LinkedList<String> params = new LinkedList<>();

    String[] paramArray = line.split(",");
    if(paramArray.length == 0) throw new SyntaxError("MissingParameter");

    for(String param: paramArray) {
      param = param.trim();
      if(param.isEmpty()) throw new SyntaxError("Missing Parameter");
      params.add(param);
    }
    return params;
  }
}
