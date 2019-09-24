package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.SyntaxError;

import java.util.LinkedList;
import java.util.List;

class ParameterParser {
  private final List<String> params;

  ParameterParser(String line) throws SyntaxError {
    params = new LinkedList<>();

    for(String param: line.split(",")) {
      param = param.trim();
      if(param.isEmpty()) throw new SyntaxError("Missing Parameter");
      params.add(param);
    }
  }

  List<String> getParams() {
    return params;
  }

  String getParam(int index) {
    if (params.size() <= index) return "";
    return params.get(index);
  }

}
