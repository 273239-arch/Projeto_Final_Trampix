import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { mensagemDoErro } from '../interceptors/erro-interceptor';
import { ErroApi } from '../models/erro-api';

// Põe os erros do back no formulário: cada item de "erros" (400) vai para o seu campo, como erro
// "servidor". Devolve a mensagem geral para mostrar no topo (422, 409, 500...) ou null se não houver.
export function aplicarErrosDoBack(form: FormGroup, erro: unknown): string | null {
  if (!(erro instanceof HttpErrorResponse)) {
    return 'Não foi possível enviar. Tente de novo.';
  }
  // 401 já leva para o login (erroInterceptor).
  if (erro.status === 401) {
    return null;
  }
  const corpo = erro.error as Partial<ErroApi> | null;
  const semCampo: string[] = [];
  for (const item of corpo?.erros ?? []) {
    const controle = form.get(item.campo);
    if (controle) {
      controle.setErrors({ ...controle.errors, servidor: maiuscula(item.mensagem) });
      controle.markAsTouched();
    } else {
      semCampo.push(maiuscula(item.mensagem));
    }
  }
  if (corpo?.erros?.length && !semCampo.length) {
    return null;
  }
  return [mensagemDoErro(erro), ...semCampo].join(' ');
}

// Mensagem do primeiro erro do campo, depois de mexer nele (o erro "servidor" traz o texto do back).
export function mensagemDoCampo(controle: AbstractControl, mensagens: Record<string, string>): string {
  const erros = controle.errors;
  if (!erros || !(controle.touched || controle.dirty)) {
    return '';
  }
  if (erros['servidor']) {
    return erros['servidor'] as string;
  }
  const chave = Object.keys(erros)[0];
  return mensagens[chave] ?? 'Valor inválido.';
}

function maiuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
