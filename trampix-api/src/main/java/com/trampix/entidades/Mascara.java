package com.trampix.entidades;

// Tira a máscara de CPF, CNPJ, CEP e telefone: o banco guarda só dígitos.
final class Mascara {

    private Mascara() {
    }

    static String soDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }
}
