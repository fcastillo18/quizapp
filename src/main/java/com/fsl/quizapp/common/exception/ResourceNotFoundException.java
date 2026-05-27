package com.fsl.quizapp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when a requested resource does not exist. Results in HTTP 404. */
public class ResourceNotFoundException extends ResponseStatusException {

  /** Creates a 404 exception for the given resource type and identifier. */
  public ResourceNotFoundException(String resource, Object id) {
    super(HttpStatus.NOT_FOUND, resource + " not found: " + id);
  }
}
