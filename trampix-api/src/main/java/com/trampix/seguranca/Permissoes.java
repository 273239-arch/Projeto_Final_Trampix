package com.trampix.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;

// Regras de "quem pode mexer em quê" usadas pelos recursos.
public final class Permissoes {

    private Permissoes() {
    }

    // Só o próprio usuário ou um ADMIN. Senão, 403.
    public static void exigirProprioOuAdmin(Usuario logado, Long usuarioId) {
        boolean proprio = logado.getId().equals(usuarioId);
        boolean admin = logado.getPerfil() == Perfil.ADMIN;
        if (!proprio && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "só o dono da conta ou um ADMIN pode fazer isso");
        }
    }
}
