package com.trampix.entidades;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;
import org.hibernate.validator.group.GroupSequenceProvider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.entidades.enums.TipoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@GroupSequenceProvider(ValidacaoDocumento.class)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    // TODO(decisão aberta): login por e-mail. O Figma usa "nome de usuário".
    @NotBlank
    @Email
    @Size(max = 160)
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    // Só o hash BCrypt vai para o banco. Nunca sai no JSON.
    @JsonIgnore
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    // Senha em texto puro: só chega na requisição, não é gravada nem devolvida.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
    private String senha;

    // Só dígitos (o setter tira a máscara). CPF tem 11 dígitos e CNPJ tem 14.
    // @CPF ou @CNPJ confere os dígitos verificadores, conforme o tipoDocumento
    // (quem escolhe qual dos dois vale é a classe ValidacaoDocumento).
    @NotBlank
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos")
    @CPF(groups = ValidacaoDocumento.Cpf.class)
    @CNPJ(groups = ValidacaoDocumento.Cnpj.class)
    @Column(nullable = false, unique = true, length = 14, columnDefinition = "CHAR(14)")
    private String documento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    // Opcional: não se aplica a quem se cadastra com CNPJ.
    @Past
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter DDD + número, só dígitos")
    @Column(length = 11)
    private String telefone;

    // USUARIO para todo cadastro. Ser prestador não é perfil: é ter um prestador_perfil.
    // Só leitura no JSON: ninguém se cadastra como ADMIN.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Perfil perfil = Perfil.USUARIO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.ATIVO;

    // Endereço separado em campos para o filtro por localização funcionar.
    @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos")
    @Column(length = 8)
    private String cep;

    @Size(max = 150)
    @Column(length = 150)
    private String logradouro;

    @Size(max = 10)
    @Column(length = 10)
    private String numero;

    @Size(max = 60)
    @Column(length = 60)
    private String complemento;

    @Size(max = 80)
    @Column(length = 80)
    private String bairro;

    @Size(max = 80)
    @Column(length = 80)
    private String cidade;

    @Pattern(regexp = "[A-Z]{2}", message = "UF deve ter 2 letras maiúsculas")
    @Column(length = 2)
    private String uf;

    // Recuperação de senha ("Esqueci minha senha"). Guarda só o hash do token.
    @JsonIgnore
    @Column(name = "token_recuperacao_hash", length = 64)
    private String tokenRecuperacaoHash;

    @JsonIgnore
    @Column(name = "token_recuperacao_expira_em")
    private LocalDateTime tokenRecuperacaoExpiraEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    // Os setters abaixo tiram pontos, traços e parênteses: o banco guarda só dígitos.
    public void setDocumento(String documento) {
        this.documento = Mascara.soDigitos(documento);
    }

    public void setTelefone(String telefone) {
        this.telefone = Mascara.soDigitos(telefone);
    }

    public void setCep(String cep) {
        this.cep = Mascara.soDigitos(cep);
    }

    public void setUf(String uf) {
        this.uf = uf == null ? null : uf.trim().toUpperCase();
    }
}
