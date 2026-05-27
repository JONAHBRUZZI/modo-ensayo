package com.modoensayo.venues.dto;

public record RoomRequest(String name, Integer capacity, String floorType, String type,
                          Boolean hasMirrors, Boolean hasSound, String equipment, Double pricePerHour) {}
