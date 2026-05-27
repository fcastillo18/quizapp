package com.fsl.quizapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test — verifies the Spring application context loads cleanly. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizappApplicationTests {

  /** Asserts the application context starts without errors. */
  @Test
  void contextLoads() {
  }
}
