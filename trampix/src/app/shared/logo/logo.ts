import { Component, input } from '@angular/core';

// Logo do Trampix: imagem da caixa de ferramentas (public/logo-trampix.png)
// + "Tram" (azul-escuro) e "pix" (verde).
// "normal": imagem à esquerda do texto (cabeçalho). "grande": imagem em cima do texto (login).
@Component({
  selector: 'app-logo',
  standalone: false,
  styleUrl: './logo.scss',
  templateUrl: './logo.html',
})
export class Logo {
  readonly tamanho = input<'normal' | 'grande'>('normal');
}
