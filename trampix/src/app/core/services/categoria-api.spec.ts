import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CategoriaApi } from './categoria-api';

describe('CategoriaApi', () => {
  let service: CategoriaApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoriaApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('faz GET /categorias', () => {
    service.listar().subscribe();
    http.expectOne(`${environment.apiUrl}/categorias`).flush([]);
  });
});
