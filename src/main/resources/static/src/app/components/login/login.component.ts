import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  template: `
    <div class="row justify-content-center">
      <div class="col-md-6">
        <div class="card">
          <div class="card-header">
            🔑 Авторизация
          </div>
          <div class="card-body">
            <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
              <div class="mb-3">
                <label for="username" class="form-label">Логин</label>
                <input 
                  type="text" 
                  class="form-control" 
                  id="username"
                  formControlName="username"
                  placeholder="Введите логин"
                >
              </div>
              
              <div class="mb-3">
                <label for="password" class="form-label">Пароль</label>
                <input 
                  type="password" 
                  class="form-control" 
                  id="password"
                  formControlName="password"
                  placeholder="Введите пароль"
                >
              </div>
              
              <div class="alert alert-info">
                <strong>Тестовые пользователи:</strong><br>
                <code>admin / admin</code> (ADMIN)<br>
                <code>user / user</code> (USER)
              </div>
              
              <div class="d-grid">
                <button type="submit" class="btn btn-primary" [disabled]="!loginForm.valid">
                  Войти
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  `,
  standalone: false,
  styles: []
})
export class LoginComponent {
  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      const { username, password } = this.loginForm.value;
      
      // Простая проверка (в реальности нужна HTTP авторизация)
      if ((username === 'admin' && password === 'admin') ||
          (username === 'user' && password === 'user')) {
        alert(`✅ Успешная авторизация!\nЛогин: ${username}\nРоль: ${username === 'admin' ? 'ADMIN' : 'USER'}`);
        this.router.navigate(['/search']);
      } else {
        alert('❌ Неверный логин или пароль');
      }
    }
  }
}
