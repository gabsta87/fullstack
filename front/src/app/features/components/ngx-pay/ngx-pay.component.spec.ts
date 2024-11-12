import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NgxPayComponent } from './ngx-pay.component';

describe('NgxPayComponent', () => {
  let component: NgxPayComponent;
  let fixture: ComponentFixture<NgxPayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NgxPayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NgxPayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
