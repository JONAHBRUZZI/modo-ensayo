package com.modoensayo.classes.dto;

import com.modoensayo.classes.enums.Disciplina;
import com.modoensayo.classes.enums.NivelClase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CursoSearchDto {

    private UUID id;
    private String nombre;
    private Disciplina disciplina;
    private NivelClase nivel;
    private BigDecimal precio;
    private Integer cuposMaximos;
    private Integer cuposOcupados;
    private Integer cuposDisponibles;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String nombreSede;
    private String nombreSala;
    private String comunaSede;
    private String regionSede;

    public CursoSearchDto() {}

    public CursoSearchDto(UUID id, String nombre, Disciplina disciplina, NivelClase nivel,
                          BigDecimal precio, Integer cuposMaximos, Integer cuposOcupados,
                          Integer cuposDisponibles, LocalDateTime fechaInicio,
                          LocalDateTime fechaFin, String nombreSede, String nombreSala,
                          String comunaSede, String regionSede) {
        this.id = id;
        this.nombre = nombre;
        this.disciplina = disciplina;
        this.nivel = nivel;
        this.precio = precio;
        this.cuposMaximos = cuposMaximos;
        this.cuposOcupados = cuposOcupados;
        this.cuposDisponibles = cuposDisponibles;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.nombreSede = nombreSede;
        this.nombreSala = nombreSala;
        this.comunaSede = comunaSede;
        this.regionSede = regionSede;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Disciplina getDisciplina() { return disciplina; }
    public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }

    public NivelClase getNivel() { return nivel; }
    public void setNivel(NivelClase nivel) { this.nivel = nivel; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getCuposMaximos() { return cuposMaximos; }
    public void setCuposMaximos(Integer cuposMaximos) { this.cuposMaximos = cuposMaximos; }

    public Integer getCuposOcupados() { return cuposOcupados; }
    public void setCuposOcupados(Integer cuposOcupados) { this.cuposOcupados = cuposOcupados; }

    public Integer getCuposDisponibles() { return cuposDisponibles; }
    public void setCuposDisponibles(Integer cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getNombreSede() { return nombreSede; }
    public void setNombreSede(String nombreSede) { this.nombreSede = nombreSede; }

    public String getNombreSala() { return nombreSala; }
    public void setNombreSala(String nombreSala) { this.nombreSala = nombreSala; }

    public String getComunaSede() { return comunaSede; }
    public void setComunaSede(String comunaSede) { this.comunaSede = comunaSede; }

    public String getRegionSede() { return regionSede; }
    public void setRegionSede(String regionSede) { this.regionSede = regionSede; }
}
