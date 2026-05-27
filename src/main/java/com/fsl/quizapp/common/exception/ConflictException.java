package com.fsl.quizapp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when a request conflicts with existing state. Results in HTTP 409. */
public class ConflictException extends ResponseStatusException {

  /** Creates a 409 exception with the given detail message. */
  public ConflictException(String detail) {
    super(HttpStatus.CONFLICT, detail);
  }
}
