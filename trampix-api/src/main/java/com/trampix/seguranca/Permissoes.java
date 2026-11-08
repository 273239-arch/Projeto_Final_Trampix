package com.trampix.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;

// regras de permissão
public final class Permissoes {

    private Permissoes() {
    }

    // só o próprio usuário ou ADMIN
    public static void exigirProprioOuAdmin(Usuario logado, Long usuarioId) {
        boolean proprio = logado.getId().equals(usuarioId);
        boolean admin = logado.getPerfil() == Perfil.ADMIN;
        if (!proprio && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "só o dono da conta ou um ADMIN pode fazer isso");
        }
    }
}
