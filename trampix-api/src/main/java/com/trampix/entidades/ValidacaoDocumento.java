package com.trampix.entidades;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.TipoDocumento;

// escolhe as validações do documento (CPF ou CNPJ)
public class ValidacaoDocumento implements DefaultGroupSequenceProvider<Usuario> {

    // grupos de validação
    public interface Obrigatorio {
    }

    public interface Cpf {
    }

    public interface Cnpj {
    }

    @Override
    public List<Class<?>> getValidationGroups(Class<?> klass, Usuario usuario) {
        List<Class<?>> grupos = new ArrayList<>();
        grupos.add(Usuario.class); // grupo padrão
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
