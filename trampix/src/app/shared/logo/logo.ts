import { Component, input } from '@angular/core';

// Logo do Figma: caixa de ferramentas + "Tram" (azul-escuro) e "pix" (verde).
// "claro" serve para fundo escuro (rodapé): o "Tram" fica branco.
@Component({
  selector: 'app-logo',
  standalone: false,
  styleUrl: './logo.scss',
  templateUrl: './logo.html',
})
export class Logo {
  readonly tamanho = input<'normal' | 'grande'>('normal');
  readonly claro = input(false);
}
