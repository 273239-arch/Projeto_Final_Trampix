package com.trampix.entidades.enums;

// Decisão: o usuário é único. Ninguém escolhe "cliente" ou "prestador" no cadastro:
// quem tem prestador_perfil (criado junto com o primeiro serviço) é prestador.
// ADMIN não é criado pelo cadastro público.
public enum Perfil {
    USUARIO,
    ADMIN
}
