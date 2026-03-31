import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

  constructor(private http: HttpClient) { }

  // Населенные пункты
  getLocalities(search?: string): Observable<ApiResponse<Locality[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<ApiResponse<Locality[]>>(`${this.baseUrl}/localities`, { params });
  }

  getLocalityById(id: number): Observable<ApiResponse<Locality>> {
    return this.http.get<ApiResponse<Locality>>(`${this.baseUrl}/localities/${id}`);
  }

  // Улицы
  getStreets(localityId: number, search?: string): Observable<ApiResponse<Street[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<ApiResponse<Street[]>>(`${this.baseUrl}/localities/${localityId}/streets`, { params });
  }

  // Дома
  getHouses(localityId: number, streetId: number, search?: string): Observable<ApiResponse<House[]>> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<ApiResponse<House[]>>(
      `${this.baseUrl}/localities/${localityId}/streets/${streetId}/houses`,
      { params }
    );
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
    return this.http.get<ApiResponse<Apartment[]>>(
      `${this.baseUrl}/localities/${localityId}/streets/${streetId}/houses/${houseId}/apartments`,
      { params }
    );
  }

  // Лицевые счета
  getAccounts(apartmentId: number): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/apartments/${apartmentId}/accounts`);
  }

  searchAccountByNumber(number: string): Observable<ApiResponse<Account>> {
    return this.http.get<ApiResponse<Account>>(`${this.baseUrl}/accounts/search`, {
      params: new HttpParams().set('number', number)
    });
  }

  // Статистика
  getStatistics(): Observable<ApiResponse<Record<string, number>>> {
    return this.http.get<ApiResponse<Record<string, number>>>(`${this.baseUrl}/statistics`);
  }
}
