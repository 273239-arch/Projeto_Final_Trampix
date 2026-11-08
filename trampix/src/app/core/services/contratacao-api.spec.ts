import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { SEM_AVISO_DE_ERRO } from '../interceptors/erro-interceptor';
import { ContratacaoApi } from './contratacao-api';

describe('ContratacaoApi', () => {
  let service: ContratacaoApi;
  let http: HttpTestingController;
  const url = `${environment.apiUrl}/contratacoes`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ContratacaoApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista pelo papel', () => {
    service.listar('prestador').subscribe();
    http.expectOne(`${url}?papel=prestador`).flush([]);
  });

  it('cria o pedido sem snackbar de erro (o formulário mostra)', () => {
    service.criar({ servicoId: 7, dataInicio: '2026-12-01' }).subscribe();
    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.context.get(SEM_AVISO_DE_ERRO)).toBe(true);
    req.flush({});
  });

  it('recusa com motivo vazio enviando null', () => {
    service.recusar(3, '').subscribe();
    const req = http.expectOne(`${url}/3/recusar`);
    expect(req.request.body).toEqual({ motivo: null });
    req.flush({});
  });

  it('usa as rotas de cada ação', () => {
    service.aceitar(1).subscribe();
    service.cancelar(2).subscribe();
    service.concluir(3).subscribe();
    service.aceitarContraproposta(4).subscribe();
    http.expectOne(`${url}/1/aceitar`).flush({});
    http.expectOne(`${url}/2/cancelar`).flush({});
    http.expectOne(`${url}/3/concluir`).flush({});
    http.expectOne(`${url}/4/aceitar-contraproposta`).flush({});
  });
});
