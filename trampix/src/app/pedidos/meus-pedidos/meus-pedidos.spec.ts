import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Contratacao } from '../../core/models/contratacao';
import { pedidoDeTeste } from '../pedido-de-teste';
import { PedidosModule } from '../pedidos-module';
import { MeusPedidos } from './meus-pedidos';

describe('MeusPedidos', () => {
  let fixture: ComponentFixture<MeusPedidos>;
  let http: HttpTestingController;
  const tela = () => fixture.nativeElement as HTMLElement;
  const botoes = () => Array.from(tela().querySelectorAll('button')).map((b) => b.textContent!.trim());

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PedidosModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(MeusPedidos);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  const responder = async (pedidos: Contratacao[]) => {
    http.expectOne(`${environment.apiUrl}/contratacoes?papel=cliente`).flush(pedidos);
    await fixture.whenStable();
  };

  it('mostra a lista vazia', async () => {
    await responder([]);
    expect(tela().textContent).toContain('Você ainda não fez nenhum pedido');
  });

  it('mostra serviço, categoria, prestador e o botão de cancelar', async () => {
    await responder([pedidoDeTeste({})]);
    // A categoria vem no próprio pedido, sem GET /servicos
    http.expectNone(`${environment.apiUrl}/servicos`);
    const texto = tela().textContent!;
    expect(texto).toContain('Pendente');
    expect(texto).toContain('Pintura de sala');
    expect(texto).toContain('Pintor › Paredes');
    expect(texto).toContain('José');
    expect(botoes()).toContain('Cancelar pedido');
  });

  it('em contraproposta mostra o motivo e os botões de resposta', async () => {
    await responder([
      pedidoDeTeste({ status: 'CONTRAPROPOSTA', motivoContraproposta: 'Só consigo na semana seguinte.' }),
    ]);
    expect(tela().textContent).toContain('Só consigo na semana seguinte.');
    expect(botoes()).toEqual(expect.arrayContaining(['Aceitar contraproposta', 'Recusar']));
    expect(botoes()).not.toContain('Cancelar pedido');

    tela()
      .querySelectorAll('button')
      .forEach((b) => b.textContent!.includes('Aceitar contraproposta') && b.click());
    http
      .expectOne(`${environment.apiUrl}/contratacoes/1/aceitar-contraproposta`)
      .flush(pedidoDeTeste({ status: 'ACEITA', prestador: { id: 9, nome: 'José', cidade: null, uf: null, telefone: '(81) 99999-0000', endereco: null } }));
    await fixture.whenStable();
    expect(tela().textContent).toContain('(81) 99999-0000');
  });

  it('mostra o motivo da recusa', async () => {
    await responder([pedidoDeTeste({ status: 'RECUSADA', motivoRecusa: 'Agenda cheia.' })]);
    expect(tela().textContent).toContain('Agenda cheia.');
    expect(botoes()).toEqual([]);
  });
});
