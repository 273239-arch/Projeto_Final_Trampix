import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NovoUsuario, Usuario } from '../models/usuario';

@Service()
export class UsuarioApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/usuarios`;

  cadastrar(usuario: NovoUsuario): Observable<Usuario> {
    return this.http.post<Usuario>(this.url, usuario);
  }
}
