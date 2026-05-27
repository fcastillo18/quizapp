package com.fsl.quizapp.common.web;

import com.fsl.quizapp.common.exception.BadRequestException;
import com.fsl.quizapp.common.exception.ConflictException;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Translates domain exceptions to RFC 7807 ProblemDetail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /** Handles 404 not found errors. */
  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
  }

  /** Handles 400 bad request errors. */
  @ExceptionHandler(BadRequestException.class)
  ProblemDetail handleBadRequest(BadRequestException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
  }

  /** Handles 409 conflict errors. */
  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getReason());
  }
}
