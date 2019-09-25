package com.endava.mmarko.assembler.parsers;

import com.endava.mmarko.assembler.config.TwoPassAssemblerConfig;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TwoPassAssemblerConfig.class)
public class InstructionParserTests {
  @Mock
  NumberParser numberParser;
  @Mock
  ParameterParser parameterParser;
  @InjectMocks
  InstructionParser instructionParser;



  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }
}
