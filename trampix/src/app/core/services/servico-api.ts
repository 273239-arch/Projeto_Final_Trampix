import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Servico } from '../models/servico';

@Service()
export class ServicoApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/servicos`;

  // GET /servicos ou GET /servicos?categoriaId=3
  listar(categoriaId?: number | null): Observable<Servico[]> {
    let params = new HttpParams();
    if (categoriaId != null) {
      params = params.set('categoriaId', categoriaId);
    }
    return this.http.get<Servico[]>(this.url, { params });
  }
}
