package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;

@RestControllerAdvice(basePackages="com.ziyi.leetcodereviewsystem.coach")
@Order(-10)
public class CoachErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) {return ResponseEntity.status(e.getStatusCode()).body(Map.of("code",Objects.toString(e.getReason(),"REQUEST_FAILED"),"requestId",UUID.randomUUID().toString()));}
    @ExceptionHandler({IllegalArgumentException.class,ArithmeticException.class,java.time.DateTimeException.class,ClassCastException.class,MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","requestId",UUID.randomUUID().toString()));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("code","CONFLICT","requestId",UUID.randomUUID().toString()));}
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<?> unavailable(org.springframework.dao.DataAccessException e){return ResponseEntity.status(503).body(Map.of("code","SAVE_UNAVAILABLE","requestId",UUID.randomUUID().toString()));}
}
