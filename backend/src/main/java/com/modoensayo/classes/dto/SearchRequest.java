package com.modoensayo.classes.dto;

import com.modoensayo.classes.enums.Disciplina;

import java.time.LocalDate;
import java.time.LocalTime;

public class SearchRequest {

    private String comuna;
    private String tipo;
    private Boolean requiereEspejos;
    private Integer capacidadMinima;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String nivel;
    private String keyword;
    private Double latitud;
    private Double longitud;
    private Double radioKm;
    private Disciplina disciplina;
    private String region;

    public SearchRequest() {}

    public SearchRequest(String comuna, String tipo, Boolean requiereEspejos,
                         Integer capacidadMinima, LocalDate fecha,
                         LocalTime horaInicio, LocalTime horaFin,
                         String nivel, String keyword,
                         Double latitud, Double longitud, Double radioKm,
                         Disciplina disciplina, String region) {
        this.comuna = comuna;
        this.tipo = tipo;
        this.requiereEspejos = requiereEspejos;
        this.capacidadMinima = capacidadMinima;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.nivel = nivel;
        this.keyword = keyword;
        this.latitud = latitud;
        this.longitud = longitud;
        this.radioKm = radioKm;
        this.disciplina = disciplina;
        this.region = region;
    }

    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Boolean getRequiereEspejos() { return requiereEspejos; }
    public void setRequiereEspejos(Boolean requiereEspejos) { this.requiereEspejos = requiereEspejos; }

    public Integer getCapacidadMinima() { return capacidadMinima; }
    public void setCapacidadMinima(Integer capacidadMinima) { this.capacidadMinima = capacidadMinima; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Double getRadioKm() { return radioKm; }
    public void setRadioKm(Double radioKm) { this.radioKm = radioKm; }

    public Disciplina getDisciplina() { return disciplina; }
    public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public boolean hasGeoLocation() {
        return latitud != null && longitud != null && radioKm != null;
    }

    public boolean hasComuna() {
        return comuna != null && !comuna.trim().isEmpty();
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    public boolean hasRegion() {
        return region != null && !region.trim().isEmpty();
    }

    public boolean hasTimeFilter() {
        return fecha != null && horaInicio != null && horaFin != null;
    }

    public void normalize() {
        if (comuna != null) this.comuna = comuna.trim();
        if (keyword != null) this.keyword = keyword.trim();
        if (region != null) this.region = region.trim();
        if (tipo != null) {
            this.tipo = tipo.toUpperCase().trim();
        }
    }
}
