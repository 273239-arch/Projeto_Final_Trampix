import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { LoginModule } from '../login-module';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('não envia o formulário vazio', () => {
    fixture.componentInstance.entrar();
    http.expectNone(`${environment.apiUrl}/auth/login`);
  });

  it('envia e-mail e senha para /auth/login', () => {
    const form = fixture.componentInstance['form'];
    form.setValue({ email: 'ana@teste.com', senha: '12345678' });
    fixture.componentInstance.entrar();
    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.body).toEqual({ email: 'ana@teste.com', senha: '12345678' });
  });
});
