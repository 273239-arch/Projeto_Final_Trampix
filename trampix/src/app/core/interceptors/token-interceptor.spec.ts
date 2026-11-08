import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Auth } from '../services/auth';
import { tokenInterceptor } from './token-interceptor';

describe('tokenInterceptor', () => {
  let http: HttpClient;
  let controle: HttpTestingController;
  let token: string | null;

  beforeEach(() => {
    token = 'abc';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { token: () => token } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controle = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controle.verify());

  it('envia Authorization: Bearer para a API', () => {
    http.get(`${environment.apiUrl}/usuarios/1`).subscribe();
    const req = controle.expectOne(`${environment.apiUrl}/usuarios/1`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
    req.flush({});
  });

  it('não envia o token sem login nem para outros sites', () => {
    http.get('https://exemplo.com/x').subscribe();
    expect(controle.expectOne('https://exemplo.com/x').request.headers.has('Authorization')).toBe(false);

    token = null;
    http.get(`${environment.apiUrl}/servicos`).subscribe();
    const req = controle.expectOne(`${environment.apiUrl}/servicos`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });
});
