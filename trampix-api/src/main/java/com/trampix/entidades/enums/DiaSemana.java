package com.trampix.entidades.enums;

import java.time.DayOfWeek;

public enum DiaSemana {
    SEGUNDA,
    TERCA,
    QUARTA,
    QUINTA,
    SEXTA,
    SABADO,
    DOMINGO;

    public static DiaSemana de(DayOfWeek dia) {
        return values()[dia.ordinal()];
    }
}
