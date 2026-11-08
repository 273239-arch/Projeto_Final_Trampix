import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SEM_AVISO_DE_ERRO } from '../interceptors/erro-interceptor';
import { Servico } from '../models/servico';

export interface FiltroServicos {
  categoriaId?: number | null;
  subcategoriaId?: number | null;
}

@Service()
export class ServicoApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/servicos`;

  // GET /servicos, com ?categoriaId= e ?subcategoriaId= opcionais.
  // semAviso: quando a lista é só um complemento da tela, a falha não vira snackbar.
  listar(filtro: FiltroServicos = {}, semAviso = false): Observable<Servico[]> {
    let params = new HttpParams();
    if (filtro.categoriaId != null) {
      params = params.set('categoriaId', filtro.categoriaId);
    }
    if (filtro.subcategoriaId != null) {
      params = params.set('subcategoriaId', filtro.subcategoriaId);
    }
    const context = new HttpContext().set(SEM_AVISO_DE_ERRO, semAviso);
    return this.http.get<Servico[]>(this.url, { params, context });
  }

  // GET /servicos/{id}: só serviço ATIVO (senão 404).
  buscar(id: number): Observable<Servico> {
    return this.http.get<Servico>(`${this.url}/${id}`);
  }
}
