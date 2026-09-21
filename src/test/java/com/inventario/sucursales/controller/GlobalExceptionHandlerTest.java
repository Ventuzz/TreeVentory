package com.inventario.sucursales.controller;

import com.inventario.sucursales.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// # Pruebas unitarias del manejador global de excepciones REST
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Credenciales invalidas");
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Credenciales invalidas", response.getBody().getMessage());
    }

    @Test
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Parametro invalido");
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Parametro invalido", response.getBody().getMessage());
    }

    @Test
    void testHandleIllegalState() {
        IllegalStateException ex = new IllegalStateException("Estado inconsistente");
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Estado inconsistente", response.getBody().getMessage());
    }

    @Test
    void testHandleGeneralException() {
        Exception ex = new RuntimeException("Error inesperado");
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void testHandleValidationExceptions() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "campo", "El campo es obligatorio");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("El campo es obligatorio", response.getBody().getData().get("campo"));
    }
}
