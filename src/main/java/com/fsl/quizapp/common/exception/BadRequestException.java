package com.fsl.quizapp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when the client sends invalid input. Results in HTTP 400. */
public class BadRequestException extends ResponseStatusException {

  /** Creates a 400 exception with the given detail message. */
  public BadRequestException(String detail) {
    super(HttpStatus.BAD_REQUEST, detail);
  }
}
