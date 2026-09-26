package com.salon.belleza.model;

/**
 * Enum para el estado de una Jornada de trabajo.
 * ABIERTA: La trabajadora está activamente trabajando en esa sede.
 * CERRADA: La jornada fue finalizada, ya sea manualmente o automáticamente
 *          por un cambio de sede. Los valores financieros quedan congelados.
 */
public enum EstadoJornada {
    ABIERTA,
    CERRADA
}
