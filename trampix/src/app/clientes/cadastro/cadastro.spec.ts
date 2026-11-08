import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ClientesModule } from '../clientes-module';
import { Cadastro } from './cadastro';

describe('Cadastro', () => {
  let component: Cadastro;
  let fixture: ComponentFixture<Cadastro>;
  let http: HttpTestingController;
  const form = () => component['form'];

  function preencher(): void {
    form().patchValue({
      nome: 'Ana Souza',
      email: 'ana@teste.com',
      senha: '12345678',
      confirmarSenha: '12345678',
      tipoDocumento: 'CPF',
      documento: '529.982.247-25',
      telefone: '(11) 99999-8888',
      aceiteDocumentos: true,
      aceiteTermos: true,
    });
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientesModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Cadastro);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  it('acusa senhas diferentes no "Confirmar senha"', () => {
    preencher();
    form().patchValue({ confirmarSenha: 'outra-senha' });
    form().controls.confirmarSenha.markAsTouched();
    expect(component.erro('confirmarSenha')).toBe('As senhas não conferem.');
  });

  it('confere o tamanho do documento conforme CPF ou CNPJ', () => {
    preencher();
    expect(form().controls.documento.valid).toBe(true);
    form().controls.tipoDocumento.setValue('CNPJ');
    expect(form().controls.documento.hasError('cnpj')).toBe(true);
  });

  it('envia só dígitos e sem "confirmarSenha"', () => {
    preencher();
    component.criarConta();
    const req = http.expectOne(`${environment.apiUrl}/usuarios`);
    expect(req.request.body).toEqual({
      nome: 'Ana Souza',
      email: 'ana@teste.com',
      senha: '12345678',
      tipoDocumento: 'CPF',
      documento: '52998224725',
      telefone: '11999998888',
      dataNascimento: null,
    });
  });

  it('mostra os erros do back embaixo de cada campo', () => {
    preencher();
    component.criarConta();
    http.expectOne(`${environment.apiUrl}/usuarios`).flush(
      { status: 400, mensagem: 'dados inválidos', erros: [{ campo: 'documento', mensagem: 'CPF inválido' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(component.erro('documento')).toBe('CPF inválido');
  });

  it('coloca o 409 de e-mail repetido no campo e-mail', () => {
    preencher();
    component.criarConta();
    http
      .expectOne(`${environment.apiUrl}/usuarios`)
      .flush({ status: 409, mensagem: 'e-mail já cadastrado' }, { status: 409, statusText: 'Conflict' });
    expect(component.erro('email')).toBe('E-mail já cadastrado');
  });
});
