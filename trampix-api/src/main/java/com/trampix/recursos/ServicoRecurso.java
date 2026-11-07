package com.trampix.recursos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.enums.StatusServico;
import com.trampix.repositorios.ServicoRepositorio;

// Busca pública de serviços (sem token). Só mostra serviços ATIVOS.
// O cadastro do primeiro serviço está em PrestadorRecurso.
// TODO(próxima etapa): POST/PUT/DELETE de serviço para quem já é prestador; filtros por cidade/UF e preço.
@RestController
@RequestMapping("/servicos")
public class ServicoRecurso {

    private final ServicoRepositorio repositorio;

    public ServicoRecurso(ServicoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /servicos ou GET /servicos?categoriaId=3
    @GetMapping
    public List<ServicoResposta> listar(@RequestParam(required = false) Long categoriaId) {
        var servicos = categoriaId == null
                ? repositorio.findByStatus(StatusServico.ATIVO)
                : repositorio.findByCategoriaIdAndStatus(categoriaId, StatusServico.ATIVO);
        return servicos.stream().map(ServicoResposta::de).toList();
    }

    @GetMapping("/{id}")
    public ServicoResposta buscar(@PathVariable Long id) {
        return repositorio.findById(id)
                .filter(s -> s.getStatus() == StatusServico.ATIVO)
                .map(ServicoResposta::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "serviço não encontrado"));
    }
}
