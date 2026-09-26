package com.salon.belleza.model;

/**
 * Enum para el estado de una Cita en el calendario.
 * PENDIENTE: La cita fue registrada pero no ha ocurrido.
 * CONFIRMADA: La clienta confirmó su asistencia.
 * COMPLETADA: El servicio se realizó satisfactoriamente.
 * CANCELADA: La cita fue cancelada por cualquier motivo.
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA
}
