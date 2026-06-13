package com.modoensayo.users.dto;

import java.util.UUID;

public class AtributosActivosDTO {

    private boolean identidadValidada;
    private boolean identidadPendiente;
    private boolean tieneReservasActivas;
    private boolean tieneAsignacionesActivas;
    private boolean esAdminSede;
    private UUID sedeId;
    private boolean esAdminGeneral;
    private String estadoUsuario;

    public boolean isIdentidadValidada() { return identidadValidada; }
    public void setIdentidadValidada(boolean identidadValidada) { this.identidadValidada = identidadValidada; }

    public boolean isIdentidadPendiente() { return identidadPendiente; }
    public void setIdentidadPendiente(boolean identidadPendiente) { this.identidadPendiente = identidadPendiente; }

    public boolean isTieneReservasActivas() { return tieneReservasActivas; }
    public void setTieneReservasActivas(boolean tieneReservasActivas) { this.tieneReservasActivas = tieneReservasActivas; }

    public boolean isTieneAsignacionesActivas() { return tieneAsignacionesActivas; }
    public void setTieneAsignacionesActivas(boolean tieneAsignacionesActivas) { this.tieneAsignacionesActivas = tieneAsignacionesActivas; }

    public boolean isEsAdminSede() { return esAdminSede; }
    public void setEsAdminSede(boolean esAdminSede) { this.esAdminSede = esAdminSede; }

    public UUID getSedeId() { return sedeId; }
    public void setSedeId(UUID sedeId) { this.sedeId = sedeId; }

    public boolean isEsAdminGeneral() { return esAdminGeneral; }
    public void setEsAdminGeneral(boolean esAdminGeneral) { this.esAdminGeneral = esAdminGeneral; }

    public String getEstadoUsuario() { return estadoUsuario; }
    public void setEstadoUsuario(String estadoUsuario) { this.estadoUsuario = estadoUsuario; }
}
