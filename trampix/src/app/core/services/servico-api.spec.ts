import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ServicoApi } from './servico-api';

describe('ServicoApi', () => {
  let service: ServicoApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ServicoApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista todos os serviços sem filtro', () => {
    service.listar().subscribe();
    const req = http.expectOne(`${environment.apiUrl}/servicos`);
    expect(req.request.params.has('categoriaId')).toBe(false);
    req.flush([]);
  });

  it('filtra por categoria com ?categoriaId=', () => {
    service.listar(3).subscribe();
    http.expectOne(`${environment.apiUrl}/servicos?categoriaId=3`).flush([]);
  });
});
