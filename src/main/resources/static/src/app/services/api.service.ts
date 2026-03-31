import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
  timestamp: number;
}

export interface Locality {
  id: number;
  name: string;
}

export interface Street {
  id: number;
  name: string;
}

export interface House {
  id: number;
  number: string;
}

export interface Apartment {
  id: number;
  number: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  payerName: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = '/api';

  constructor(private http: HttpClient) { 
    console.log('ApiService initialized with baseUrl:', this.baseUrl);
  }

  // Населенные пункты
  getLocalities(search?: string): Observable<ApiResponse<Locality[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    const url = `${this.baseUrl}/localities`;
    console.log('GET Localities:', url, 'params:', params.toString());
    return this.http.get<ApiResponse<Locality[]>>(url, { params });
  }

  getLocalityById(id: number): Observable<ApiResponse<Locality>> {
    console.log('GET Locality by ID:', id);
    return this.http.get<ApiResponse<Locality>>(`${this.baseUrl}/localities/${id}`);
  }

  // Улицы
  getStreets(localityId: number, search?: string): Observable<ApiResponse<Street[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    const url = `${this.baseUrl}/localities/${localityId}/streets`;
    console.log('GET Streets:', url, 'params:', params.toString());
    return this.http.get<ApiResponse<Street[]>>(url, { params });
  }

  // Дома
  getHouses(localityId: number, streetId: number, search?: string): Observable<ApiResponse<House[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    const url = `${this.baseUrl}/localities/${localityId}/streets/${streetId}/houses`;
    console.log('GET Houses:', url, 'params:', params.toString());
    return this.http.get<ApiResponse<House[]>>(url, { params });
  }

  // Квартиры
  getApartments(
    localityId: number,
    streetId: number,
    houseId: number,
    search?: string
  ): Observable<ApiResponse<Apartment[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    const url = `${this.baseUrl}/localities/${localityId}/streets/${streetId}/houses/${houseId}/apartments`;
    console.log('GET Apartments:', url, 'params:', params.toString());
    return this.http.get<ApiResponse<Apartment[]>>(url, { params });
  }

  // Лицевые счета
  getAccounts(apartmentId: number): Observable<ApiResponse<Account[]>> {
    console.log('GET Accounts for apartmentId:', apartmentId);
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/apartments/${apartmentId}/accounts`);
  }

  searchAccountByNumber(number: string): Observable<ApiResponse<Account>> {
    console.log('GET Account by number:', number);
    return this.http.get<ApiResponse<Account>>(`${this.baseUrl}/accounts/search`, {
      params: new HttpParams().set('number', number)
    });
  }

  // Статистика
  getStatistics(): Observable<ApiResponse<Record<string, number>>> {
    console.log('GET Statistics');
    return this.http.get<ApiResponse<Record<string, number>>>(`${this.baseUrl}/statistics`);
  }
}
