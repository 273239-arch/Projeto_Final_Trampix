package com.trampix.recursos;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.ConstraintViolationException;

// transforma as exceções em respostas JSON
@RestControllerAdvice
public class TratadorErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    public record ErroCampo(String campo, String mensagem) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RespostaErro(int status, String mensagem, List<ErroCampo> erros) {
    }

    // erro de validação do corpo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> validacao(MethodArgumentNotValidException ex) {
        List<ErroCampo> erros = new ArrayList<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.add(new ErroCampo(erro.getField(), erro.getDefaultMessage()));
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> erros.add(new ErroCampo(erro.getObjectName(), erro.getDefaultMessage())));
        return resposta(HttpStatus.BAD_REQUEST, "dados inválidos", erros);
    }

    // erro de validação do JPA
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespostaErro> validacaoJpa(ConstraintViolationException ex) {
        List<ErroCampo> erros = ex.getConstraintViolations().stream()
                .map(v -> new ErroCampo(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "dados inválidos", erros);
    }

    // JSON inválido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> jsonInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "corpo da requisição inválido: confira o JSON e os valores dos campos", null);
    }

    // parâmetro com tipo errado
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespostaErro> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "dados inválidos",
                List.of(new ErroCampo(ex.getName(), "valor inválido")));
    }

    // conflito no banco
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaErro> conflito(DataIntegrityViolationException ex) {
        return resposta(HttpStatus.CONFLICT, "o registro conflita com outro já existente", null);
    }

    // sem token ou token inválido
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespostaErro> naoAutenticado(AuthenticationException ex) {
        String mensagem = ex instanceof InsufficientAuthenticationException
                ? "autenticação necessária: envie o token no header Authorization"
                : "token inválido ou expirado";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(new RespostaErro(401, mensagem, null));
    }

    // sem permissão
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErro> semPermissao(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, "você não tem permissão para esta ação", null);
    }

    // demais erros
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> geral(Exception ex) {
        if (ex instanceof ResponseStatusException rse) {
            return resposta(rse.getStatusCode(), rse.getReason(), null);
        }
        if (ex instanceof ErrorResponse er) {
            return resposta(er.getStatusCode(), er.getBody().getDetail(), null);
        }
        log.error("Erro não tratado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "erro interno no servidor", null);
    }

    private ResponseEntity<RespostaErro> resposta(HttpStatusCode status, String mensagem, List<ErroCampo> erros) {
        return ResponseEntity.status(status).body(new RespostaErro(status.value(), mensagem, erros));
    }
}
