package com.trampix.entidades;

// tira a máscara e deixa só os dígitos
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
