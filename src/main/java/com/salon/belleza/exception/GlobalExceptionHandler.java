package com.salon.belleza.exception;

import com.salon.belleza.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API REST.
 *
 * Intercepta todas las excepciones lanzadas desde los Controllers/Services
 * y las convierte en respuestas HTTP estandarizadas usando {@link ErrorResponse}.
 *
 * El frontend siempre recibe la misma estructura:
 * { "status": 404, "mensaje": "...", "timestamp": 1234567890 }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // EXCEPCIONES DE NEGOCIO PROPIAS
    // =========================================================================

    /**
     * Recurso no encontrado → 404 Not Found
     * Ej: sede, usuario, jornada, servicio con ID inexistente.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * Violación de regla de negocio → 409 Conflict
     * Ej: jornada ya abierta, username duplicado, servicio en jornada cerrada.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Excepción de negocio: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Sin permisos para la operación → 403 Forbidden
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    // =========================================================================
    // EXCEPCIONES DE VALIDACIÓN (@Valid / @Validated)
    // =========================================================================

    /**
     * Error de validación de campos en el body del request → 400 Bad Request
     * Captura los errores de @NotBlank, @NotNull, @Size, etc. en los DTOs.
     * Concatena todos los mensajes de error en una sola respuesta.
     *
     * Ej: "nombre: El nombre es obligatorio; precio: El precio debe ser mayor que cero"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Error de validación: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errores));
    }

    /**
     * Error de validación en parámetros de ruta o query (@RequestParam, @PathVariable) → 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String errores = ex.getConstraintViolations()
                .stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("Violación de constraint: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errores));
    }

    /**
     * Tipo de parámetro inválido en la URL → 400
     * Ej: /api/sedes/abc cuando se espera un Long.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = String.format(
                "El parámetro '%s' recibió el valor '%s', que no es del tipo esperado '%s'",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido"
        );
        log.warn("Tipo de parámetro inválido: {}", mensaje);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    // =========================================================================
    // EXCEPCIONES DE SEGURIDAD (Spring Security)
    // =========================================================================

    /**
     * Credenciales incorrectas en el login → 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Credenciales incorrectas: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "Usuario o contraseña incorrectos"));
    }

    /**
     * Acceso denegado por Spring Security → 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Spring Security - Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(),
                        "No tienes permisos para realizar esta acción"));
    }

    // =========================================================================
    // EXCEPCIONES DE BASE DE DATOS
    // =========================================================================

    /**
     * Violación de integridad referencial o constraint único en BD → 409 Conflict
     * Ej: intentar insertar un username duplicado que llegó antes de la validación del Service.
     * Es la segunda línea de defensa tras las validaciones del Service.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Violación de integridad en BD: {}", ex.getMostSpecificCause().getMessage());

        String mensaje = "Operación rechazada por conflicto de datos. " +
                "Es posible que ya exista un registro con esos datos.";

        // Detectar el tipo de constraint violado para dar un mensaje más específico
        String causeMsg = ex.getMostSpecificCause().getMessage();
        if (causeMsg != null) {
            if (causeMsg.contains("uk_usuario_username")) {
                mensaje = "Ya existe un usuario con ese username.";
            } else if (causeMsg.contains("uk_jornada_usuario_abierta")) {
                mensaje = "La trabajadora ya tiene una jornada abierta. Ciérrala antes de abrir otra.";
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), mensaje));
    }

    // =========================================================================
    // CAPTURA GLOBAL (safety net)
    // =========================================================================

    /**
     * Cualquier excepción no prevista → 500 Internal Server Error
     * Nunca devuelve el stack trace al cliente por seguridad.
     * El stack trace completo se registra en el log del servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Ocurrió un error interno. Por favor contacta al administrador."));
    }
}
