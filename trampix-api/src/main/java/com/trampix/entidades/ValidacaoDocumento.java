package com.trampix.entidades;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import com.trampix.entidades.enums.TipoDocumento;

// Escolhe a validação do documento conforme o tipo: só @CPF ou só @CNPJ entra em ação.
public class ValidacaoDocumento implements DefaultGroupSequenceProvider<Usuario> {

    // Grupos de validação usados em Usuario.documento.
    public interface Cpf {
    }

    public interface Cnpj {
    }

    @Override
    public List<Class<?>> getValidationGroups(Class<?> klass, Usuario usuario) {
        List<Class<?>> grupos = new ArrayList<>();
        grupos.add(Usuario.class); // o grupo padrão da própria classe
        if (usuario != null && usuario.getTipoDocumento() == TipoDocumento.CPF) {
            grupos.add(Cpf.class);
        } else if (usuario != null && usuario.getTipoDocumento() == TipoDocumento.CNPJ) {
            grupos.add(Cnpj.class);
        }
        return grupos;
    }
}
