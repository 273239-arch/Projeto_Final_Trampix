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

// busca de serviços
// TODO: cadastrar, editar e excluir serviço; filtros por cidade e preço
@RestController
@RequestMapping("/servicos")
public class ServicoRecurso {

    private final ServicoRepositorio repositorio;

    public ServicoRecurso(ServicoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // lista os serviços ativos (filtros opcionais por categoria e subcategoria)
    @GetMapping
    public List<ServicoResposta> listar(@RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId) {
        return repositorio.filtrar(StatusServico.ATIVO, categoriaId, subcategoriaId).stream()
                .map(ServicoResposta::de).toList();
    }

    @GetMapping("/{id}")
    public ServicoResposta buscar(@PathVariable Long id) {
        return repositorio.findById(id)
                .filter(s -> s.getStatus() == StatusServico.ATIVO)
                .map(ServicoResposta::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "serviço não encontrado"));
    }
}
