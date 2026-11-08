import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SEM_AVISO_DE_ERRO } from '../interceptors/erro-interceptor';
import { Contraproposta, Contratacao, NovaContratacao, Papel } from '../models/contratacao';

// Os formulários mostram o erro do back no próprio formulário, sem snackbar.
const ERRO_NO_FORMULARIO = { context: new HttpContext().set(SEM_AVISO_DE_ERRO, true) };

@Service()
export class ContratacaoApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/contratacoes`;

  criar(pedido: NovaContratacao): Observable<Contratacao> {
    return this.http.post<Contratacao>(this.url, pedido, ERRO_NO_FORMULARIO);
  }

  // GET /contratacoes?papel=cliente (meus pedidos) ou ?papel=prestador (pedidos recebidos).
  listar(papel: Papel): Observable<Contratacao[]> {
    return this.http.get<Contratacao[]>(this.url, { params: { papel } });
  }

  aceitar(id: number): Observable<Contratacao> {
    return this.http.post<Contratacao>(`${this.url}/${id}/aceitar`, null);
  }

  // Prestador recusa o pedido ou cliente recusa a contraproposta. Motivo opcional.
  recusar(id: number, motivo?: string | null): Observable<Contratacao> {
    return this.http.post<Contratacao>(`${this.url}/${id}/recusar`, { motivo: motivo || null });
  }

  contrapropor(id: number, proposta: Contraproposta): Observable<Contratacao> {
    return this.http.post<Contratacao>(`${this.url}/${id}/contraproposta`, proposta, ERRO_NO_FORMULARIO);
  }

  aceitarContraproposta(id: number): Observable<Contratacao> {
    return this.http.post<Contratacao>(`${this.url}/${id}/aceitar-contraproposta`, null);
  }

  cancelar(id: number): Observable<Contratacao> {
    return this.http.post<Contratacao>(`${this.url}/${id}/cancelar`, null);
  }

  concluir(id: number): Observable<Contratacao> {
    return this.http.post<Contratacao>(`${this.url}/${id}/concluir`, null);
  }
}
