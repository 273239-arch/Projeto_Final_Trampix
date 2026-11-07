package com.trampix.entidades;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.TipoDocumento;

// Escolhe as validações do documento conforme o usuário:
// - documento e tipoDocumento são obrigatórios para USUARIO (não para o ADMIN de sistema);
// - vale só @CPF ou só @CNPJ, conforme o tipoDocumento.
public class ValidacaoDocumento implements DefaultGroupSequenceProvider<Usuario> {

    // Grupos de validação usados em Usuario.documento e Usuario.tipoDocumento.
    public interface Obrigatorio {
    }

    public interface Cpf {
    }

    public interface Cnpj {
    }

    @Override
    public List<Class<?>> getValidationGroups(Class<?> klass, Usuario usuario) {
        List<Class<?>> grupos = new ArrayList<>();
        grupos.add(Usuario.class); // o grupo padrão da própria classe
        if (usuario == null) {
            return grupos;
        }
        if (usuario.getPerfil() != Perfil.ADMIN) {
            grupos.add(Obrigatorio.class);
        }
        if (usuario.getTipoDocumento() == TipoDocumento.CPF) {
            grupos.add(Cpf.class);
        } else if (usuario.getTipoDocumento() == TipoDocumento.CNPJ) {
            grupos.add(Cnpj.class);
        }
        return grupos;
    }
}
