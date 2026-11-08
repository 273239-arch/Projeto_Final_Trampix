package com.trampix.recursos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.repositorios.UsuarioRepositorio;
import com.trampix.seguranca.Permissoes;

import jakarta.validation.Valid;

// endpoints de usuário
@RestController
@RequestMapping("/usuarios")
public class UsuarioRecurso {

    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioRecurso(UsuarioRepositorio repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Usuario> listar() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public Usuario buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Permissoes.exigirProprioOuAdmin(logado, id);
        return buscarOu404(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario cadastrar(@Valid @RequestBody Usuario usuario) {
        if (usuario.getSenha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha é obrigatória");
        }
        if (repositorio.existsByEmail(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "e-mail já cadastrado");
        }
        if (repositorio.existsByDocumento(usuario.getDocumento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "documento já cadastrado");
        }
        usuario.setId(null);
        usuario.setPerfil(Perfil.USUARIO);
        usuario.setStatus(StatusUsuario.ATIVO);
        usuario.setSenhaHash(passwordEncoder.encode(usuario.getSenha()));
        return repositorio.save(usuario);
    }

    // atualiza os dados do usuário
    @PutMapping("/{id}")
    public Usuario atualizar(@PathVariable Long id, @Valid @RequestBody Usuario dados,
            @AuthenticationPrincipal Usuario logado) {
        Permissoes.exigirProprioOuAdmin(logado, id);
        Usuario usuario = buscarOu404(id);
        if (repositorio.existsByEmailAndIdNot(dados.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "e-mail já cadastrado");
        }
        usuario.setNome(dados.getNome());
        usuario.setEmail(dados.getEmail());
        usuario.setDataNascimento(dados.getDataNascimento());
        usuario.setTelefone(dados.getTelefone());
        usuario.setCep(dados.getCep());
        usuario.setLogradouro(dados.getLogradouro());
        usuario.setNumero(dados.getNumero());
        usuario.setComplemento(dados.getComplemento());
        usuario.setBairro(dados.getBairro());
        usuario.setCidade(dados.getCidade());
        usuario.setUf(dados.getUf());
        if (dados.getSenha() != null) {
            usuario.setSenhaHash(passwordEncoder.encode(dados.getSenha()));
        }
        return repositorio.save(usuario);
    }

    // marca a conta como inativa
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Permissoes.exigirProprioOuAdmin(logado, id);
        Usuario usuario = buscarOu404(id);
        usuario.setStatus(StatusUsuario.INATIVO);
        repositorio.save(usuario);
    }

    private Usuario buscarOu404(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado"));
    }
}
