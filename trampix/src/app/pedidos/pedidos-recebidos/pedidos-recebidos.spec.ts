import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Contratacao } from '../../core/models/contratacao';
import { pedidoDeTeste } from '../pedido-de-teste';
import { PedidosModule } from '../pedidos-module';
import { PedidosRecebidos } from './pedidos-recebidos';

describe('PedidosRecebidos', () => {
  let fixture: ComponentFixture<PedidosRecebidos>;
  let http: HttpTestingController;
  const tela = () => fixture.nativeElement as HTMLElement;
  const botao = (texto: string) =>
    Array.from(tela().querySelectorAll('button')).find((b) => b.textContent!.trim() === texto);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PedidosModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(PedidosRecebidos);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  const responder = async (pedidos: Contratacao[]) => {
    http.expectOne(`${environment.apiUrl}/contratacoes?papel=prestador`).flush(pedidos);
    http.expectOne(`${environment.apiUrl}/servicos`).flush([]);
    await fixture.whenStable();
  };

  it('pendente: aceitar, recusar e contrapropor', async () => {
    await responder([pedidoDeTeste({ observacao: 'Sala de 20 m².' })]);
    expect(tela().textContent).toContain('Maria');
    expect(tela().textContent).toContain('Sala de 20 m².');
    expect(botao('Aceitar')).toBeTruthy();
    expect(botao('Recusar')).toBeTruthy();
    expect(botao('Contrapropor')).toBeTruthy();
  });

  it('recusa com o motivo digitado', async () => {
    await responder([pedidoDeTeste({})]);
    botao('Recusar')!.click();
    await fixture.whenStable();
    const motivo = tela().querySelector<HTMLTextAreaElement>('#motivo-1')!;
    motivo.value = 'Não atendo em Olinda.';
    motivo.dispatchEvent(new Event('input'));
    botao('Recusar pedido')!.click();
    const req = http.expectOne(`${environment.apiUrl}/contratacoes/1/recusar`);
    expect(req.request.body).toEqual({ motivo: 'Não atendo em Olinda.' });
    req.flush(pedidoDeTeste({ status: 'RECUSADA', motivoRecusa: 'Não atendo em Olinda.' }));
    await fixture.whenStable();
    expect(tela().textContent).toContain('Recusada');
  });

  it('contraproposta exige motivo de 10 caracteres', async () => {
    await responder([pedidoDeTeste({})]);
    botao('Contrapropor')!.click();
    await fixture.whenStable();
    const motivo = tela().querySelector<HTMLTextAreaElement>('#cp-motivo-1')!;
    motivo.value = 'curto';
    motivo.dispatchEvent(new Event('input'));
    tela().querySelector('app-form-contraproposta form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    http.expectNone(`${environment.apiUrl}/contratacoes/1/contraproposta`);
    expect(tela().textContent).toContain('entre 10 e 500 caracteres');
  });

  it('aceita: concluir fica bloqueado antes da data de início e mostra o telefone do cliente', async () => {
    await responder([
      pedidoDeTeste({
        status: 'ACEITA',
        cliente: { id: 2, nome: 'Maria', cidade: 'Olinda', uf: 'PE', telefone: '(81) 98888-7777' },
      }),
    ]);
    expect(botao('Concluir')!.disabled).toBe(true);
    expect(botao('Cancelar')).toBeTruthy();
    expect(tela().textContent).toContain('(81) 98888-7777');
    expect(tela().textContent).toContain('Olinda - PE');
  });
});
