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

  it('mostra "Trampix"', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Trampix');
  });
});
