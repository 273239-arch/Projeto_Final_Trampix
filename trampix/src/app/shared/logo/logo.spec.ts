import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SharedModule } from '../shared-module';
import { Logo } from './logo';

describe('Logo', () => {
  let fixture: ComponentFixture<Logo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedModule],
    }).compileComponents();

    fixture = TestBed.createComponent(Logo);
    await fixture.whenStable();
  });

  it('mostra a imagem do logo e o texto "Trampix"', () => {
    const tela = fixture.nativeElement as HTMLElement;
    const imagem = tela.querySelector('img');
    expect(imagem?.getAttribute('src')).toBe('logo-trampix.png');
    expect(imagem?.getAttribute('alt')).toBe('Trampix');
    expect(tela.querySelector('.texto')?.textContent).toBe('Trampix');
  });
});
