import { Component } from '@angular/core';

@Component({
  selector: 'app-cadastro',
  standalone: false,
  styleUrl: './cadastro.scss',
  templateUrl: './cadastro.html',
})
export class Cadastro {
  cpf = '';
  nome = '';
  email = '';
  senha = '';
  confirmarSenha = '';
  mensagem = '';

  salvar(): void {
    if (!this.nome.trim()) return;
    this.mensagem = `Cliente "${this.nome.trim()}" validado! `;
  }

  limpar(): void {
    this.cpf = '';
    this.nome = '';
    this.email = '';
    this.senha = '';
    this.confirmarSenha = '';
    this.mensagem = '';
  }
}