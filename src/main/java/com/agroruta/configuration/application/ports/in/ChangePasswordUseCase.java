package com.agroruta.configuration.application.ports.in;

public interface ChangePasswordUseCase {
    void cambiarPassword(Long id, String passwordActual, String nuevaPassword);
}