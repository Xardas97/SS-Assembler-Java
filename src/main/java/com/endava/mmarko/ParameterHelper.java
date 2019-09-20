package com.endava.mmarko;

import java.util.LinkedList;
import java.util.List;

class ParameterHelper {
  private final List<String> params;

  ParameterHelper(String line) throws SyntaxError {
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
