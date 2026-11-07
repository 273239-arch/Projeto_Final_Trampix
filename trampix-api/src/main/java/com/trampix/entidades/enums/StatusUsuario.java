package com.trampix.entidades.enums;

// Exclusão lógica: a conta nunca é apagada, só muda de status.
// SUSPENSO e BANIDO serão aplicados pelas penalidades de denúncia (RF-05).
public enum StatusUsuario {
    ATIVO,
    INATIVO,
    SUSPENSO,
    BANIDO
}
