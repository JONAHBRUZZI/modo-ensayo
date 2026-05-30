package com.modoensayo.venues.enums;

public enum EstadoSede {
    PENDIENTE_APROBACION,
    APROBADA,
    RECHAZADA,
    /**
     * Sede previamente APROBADA que el Admin General ha suspendido temporalmente.
     * Sus salas no pueden recibir nuevas reservas y la sede deja de aparecer en
     * listados publicos. El estado es reversible: puede volver a APROBADA desde
     * el panel admin (boton Reactivar).
     */
    SUSPENDIDA
}
