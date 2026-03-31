import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';

import { ApiService, Locality, Street, House, Apartment, Account } from '../services/api.service';

interface SearchResult {
  localities?: Locality[];
  streets?: Street[];
  houses?: House[];
  apartments?: Apartment[];
  accounts?: Account[];
}

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {
  searchForm!: FormGroup;
  
  // Отдельные FormControl для каждого поля
  localityControl = new FormControl();
  streetControl = new FormControl();
  houseControl = new FormControl();
  apartmentControl = new FormControl();
  accountControl = new FormControl();

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService
  ) { }

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      localityId: [null],
      streetId: [null],
      houseId: [null],
      apartmentId: [null],
      accountId: [null]
    });

    // Подписка на изменения населенного пункта
    this.localityControl.valueChanges.subscribe(locality => {
      if (locality && typeof locality === 'object') {
        this.searchForm.patchValue({ localityId: locality.id });
        this.streetControl.setValue(null);
        this.houseControl.setValue(null);
        this.apartmentControl.setValue(null);
        this.accountControl.setValue(null);
      }
    });

    // Подписка на изменения улицы
    this.streetControl.valueChanges.subscribe(street => {
      if (street && typeof street === 'object') {
        this.searchForm.patchValue({ streetId: street.id });
        this.houseControl.setValue(null);
        this.apartmentControl.setValue(null);
        this.accountControl.setValue(null);
      }
    });

    // Подписка на изменения дома
    this.houseControl.valueChanges.subscribe(house => {
      if (house && typeof house === 'object') {
        this.searchForm.patchValue({ houseId: house.id });
        this.apartmentControl.setValue(null);
        this.accountControl.setValue(null);
      }
    });

    // Подписка на изменения квартиры
    this.apartmentControl.valueChanges.subscribe(apartment => {
      if (apartment && typeof apartment === 'object') {
        this.searchForm.patchValue({ apartmentId: apartment.id });
        this.accountControl.setValue(null);
      }
    });

    // Подписка на изменения лицевого счёта
    this.accountControl.valueChanges.subscribe(account => {
      if (account && typeof account === 'object') {
        this.searchForm.patchValue({ accountId: account.id });
      }
    });
  }

  // Геттеры для ID
  get localityId(): number {
    const value = this.searchForm.get('localityId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get streetId(): number {
    const value = this.searchForm.get('streetId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get houseId(): number {
    const value = this.searchForm.get('houseId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get apartmentId(): number {
    const value = this.searchForm.get('apartmentId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get accountId(): number {
    const value = this.searchForm.get('accountId')?.value;
    return typeof value === 'number' ? value : null;
  }

  getLocalities = (term$: Observable<string>): Observable<Locality[]> => {
    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        if (term.length < 2) {
          return of([]);
        }
        return this.apiService.getLocalities(term).pipe(
          tap(response => console.log('Localities:', response)),
          switchMap(response => of(response.data || []))
        );
      })
    );
  }

  getStreets = (term$: Observable<string>): Observable<Street[]> => {
    if (!this.localityId) {
      return of([]);
    }

    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        if (term.length < 2) {
          return this.apiService.getStreets(this.localityId).pipe(
            tap(response => console.log('Streets:', response)),
            switchMap(response => of(response.data || []))
          );
        }
        return this.apiService.getStreets(this.localityId, term).pipe(
          tap(response => console.log('Streets:', response)),
          switchMap(response => of(response.data || []))
        );
      })
    );
  }

  getHouses = (term$: Observable<string>): Observable<House[]> => {
    if (!this.streetId) {
      return of([]);
    }

    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        if (term.length < 1) {
          return this.apiService.getHouses(this.localityId, this.streetId).pipe(
            tap(response => console.log('Houses:', response)),
            switchMap(response => of(response.data || []))
          );
        }
        return this.apiService.getHouses(this.localityId, this.streetId, term).pipe(
          tap(response => console.log('Houses:', response)),
          switchMap(response => of(response.data || []))
        );
      })
    );
  }

  getApartments = (term$: Observable<string>): Observable<Apartment[]> => {
    if (!this.houseId) {
      return of([]);
    }

    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        if (term.length < 1) {
          return this.apiService.getApartments(this.localityId, this.streetId, this.houseId).pipe(
            tap(response => console.log('Apartments:', response)),
            switchMap(response => of(response.data || []))
          );
        }
        return this.apiService.getApartments(this.localityId, this.streetId, this.houseId, term).pipe(
          tap(response => console.log('Apartments:', response)),
          switchMap(response => of(response.data || []))
        );
      })
    );
  }

  getAccounts = (term$: Observable<string>): Observable<Account[]> => {
    if (!this.apartmentId) {
      return of([]);
    }

    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        if (term.length < 1) {
          return this.apiService.getAccounts(this.apartmentId).pipe(
            tap(response => console.log('Accounts:', response)),
            switchMap(response => of(response.data || []))
          );
        }
        return this.apiService.searchAccountByNumber(term).pipe(
          tap(response => console.log('Account:', response)),
          switchMap(response => {
            if (response.data) {
              return of([response.data]);
            }
            return of([]);
          })
        );
      })
    );
  }

  // Форматтеры для отображения
  localityFormatter = (result: Locality) => result.name;
  streetFormatter = (result: Street) => result.name;
  houseFormatter = (result: House) => result.number;
  apartmentFormatter = (result: Apartment) => result.number;
  accountFormatter = (result: Account) => `${result.accountNumber} — ${result.payerName}`;

  onSubmit(): void {
    if (this.searchForm.valid) {
      console.log('Form submitted:', this.searchForm.value);
      // Здесь будет логика поиска/отображения результатов
    }
  }
}
