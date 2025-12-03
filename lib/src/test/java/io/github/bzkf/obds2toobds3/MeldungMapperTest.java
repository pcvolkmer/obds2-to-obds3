package io.github.bzkf.obds2toobds3;

import org.junit.jupiter.api.BeforeEach;

class MeldungMapperTest {

  MeldungMapper mapper;

  @BeforeEach
  void setUp() {
    this.mapper = new MeldungMapper(false, false);
  }
}
