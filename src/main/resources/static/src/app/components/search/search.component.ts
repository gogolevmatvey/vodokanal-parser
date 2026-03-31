import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, tap, mergeMap } from 'rxjs/operators';
import { Observable, of, Subject, merge } from 'rxjs';

import { ApiService, Locality, Street, House, Apartment, Account } from '../../services/api.service';

type FormControlValue = string | Locality | Street | House | Apartment | Account | null;

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  standalone: false,
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {
  searchForm!: FormGroup;
  
  // Subject для триггера загрузки счетов
  private accountsTrigger$ = new Subject<void>();
  
  // Отдельные FormControl для каждого поля
  localityControl = new FormControl<FormControlValue>('');
  streetControl = new FormControl<FormControlValue>('');
  houseControl = new FormControl<FormControlValue>('');
  apartmentControl = new FormControl<FormControlValue>('');
  accountControl = new FormControl<FormControlValue>('');

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
    this.localityControl.valueChanges.subscribe((locality: FormControlValue) => {
      console.log('Изменение населенного пункта:', locality);
      if (locality && typeof locality === 'object' && 'id' in locality) {
        const localityId = (locality as Locality).id;
        console.log('Выбран ID населенного пункта:', localityId);
        this.searchForm.patchValue({ localityId: localityId });
        this.streetControl.setValue('');
        this.houseControl.setValue('');
        this.apartmentControl.setValue('');
        this.accountControl.setValue('');
      }
    });

    // Подписка на изменения улицы
    this.streetControl.valueChanges.subscribe((street: FormControlValue) => {
      if (street && typeof street === 'object' && 'id' in street) {
        this.searchForm.patchValue({ streetId: (street as Street).id });
        this.houseControl.setValue('');
        this.apartmentControl.setValue('');
        this.accountControl.setValue('');
      }
    });

    // Подписка на изменения дома
    this.houseControl.valueChanges.subscribe((house: FormControlValue) => {
      if (house && typeof house === 'object' && 'id' in house) {
        this.searchForm.patchValue({ houseId: (house as House).id });
        this.apartmentControl.setValue('');
        this.accountControl.setValue('');
      }
    });

    // Подписка на изменения квартиры
    this.apartmentControl.valueChanges.subscribe((apartment: FormControlValue) => {
      console.log('Изменение квартиры:', apartment);
      if (apartment && typeof apartment === 'object' && 'id' in apartment) {
        const apartmentId = (apartment as Apartment).id;
        console.log('Выбран ID квартиры:', apartmentId);
        this.searchForm.patchValue({ apartmentId: apartmentId });
        this.accountControl.setValue('');
        
        // Триггерим загрузку счетов
        this.accountsTrigger$.next();
      }
    });

    // Подписка на изменения лицевого счёта
    this.accountControl.valueChanges.subscribe((account: FormControlValue) => {
      if (account && typeof account === 'object' && 'id' in account) {
        this.searchForm.patchValue({ accountId: (account as Account).id });
      }
    });
  }

  // Геттеры для ID
  get localityId(): number | null {
    const value = this.searchForm.get('localityId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get streetId(): number | null {
    const value = this.searchForm.get('streetId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get houseId(): number | null {
    const value = this.searchForm.get('houseId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get apartmentId(): number | null {
    const value = this.searchForm.get('apartmentId')?.value;
    return typeof value === 'number' ? value : null;
  }

  get accountId(): number | null {
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
          switchMap((response: any) => of(response.data || []))
        );
      })
    );
  }

  getStreets = (term$: Observable<string>): Observable<Street[]> => {
    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        // Получаем localityId из формы в момент вызова
        const localityId = this.searchForm.get('localityId')?.value;
        console.log('getStreets: localityId из формы =', localityId);
        
        if (!localityId) {
          console.log('getStreets: нет ID населенного пункта, возвращаем пустой список');
          return of([]);
        }
        
        console.log('getStreets: термин поиска =', term);
        if (term.length < 2) {
          console.log('getStreets: термин слишком короткий, получаем все улицы');
          return this.apiService.getStreets(localityId).pipe(
            tap(response => console.log('Улицы (все):', response)),
            switchMap((response: any) => of(response.data || []))
          );
        }
        console.log('getStreets: поиск по термину:', term);
        return this.apiService.getStreets(localityId, term).pipe(
          tap(response => console.log('Улицы (поиск):', response)),
          switchMap((response: any) => of(response.data || []))
        );
      })
    );
  }

  getHouses = (term$: Observable<string>): Observable<House[]> => {
    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        const streetId = this.searchForm.get('streetId')?.value;
        const localityId = this.searchForm.get('localityId')?.value;
        console.log('getHouses: streetId =', streetId, 'localityId =', localityId);
        
        if (!streetId) {
          console.log('getHouses: нет ID улицы, возвращаем пустой список');
          return of([]);
        }
        
        console.log('getHouses: термин поиска =', term);
        if (term.length < 1) {
          return this.apiService.getHouses(localityId, streetId).pipe(
            tap(response => console.log('Дома (все):', response)),
            switchMap((response: any) => of(response.data || []))
          );
        }
        return this.apiService.getHouses(localityId, streetId, term).pipe(
          tap(response => console.log('Дома (поиск):', response)),
          switchMap((response: any) => of(response.data || []))
        );
      })
    );
  }

  getApartments = (term$: Observable<string>): Observable<Apartment[]> => {
    return term$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        const houseId = this.searchForm.get('houseId')?.value;
        const streetId = this.searchForm.get('streetId')?.value;
        const localityId = this.searchForm.get('localityId')?.value;
        console.log('getApartments: houseId =', houseId, 'streetId =', streetId, 'localityId =', localityId);
        
        if (!houseId) {
          console.log('getApartments: нет ID дома, возвращаем пустой список');
          return of([]);
        }
        
        console.log('getApartments: термин поиска =', term);
        if (term.length < 1) {
          return this.apiService.getApartments(localityId, streetId, houseId).pipe(
            tap(response => console.log('Квартиры (все):', response)),
            switchMap((response: any) => of(response.data || []))
          );
        }
        return this.apiService.getApartments(localityId, streetId, houseId, term).pipe(
          tap(response => console.log('Квартиры (поиск):', response)),
          switchMap((response: any) => of(response.data || []))
        );
      })
    );
  }

  getAccounts = (term$: Observable<string>): Observable<Account[]> => {
    // Объединяем поток ввода с потоком триггера от выбора квартиры
    const merged$ = merge(
      term$.pipe(debounceTime(300), distinctUntilChanged()),
      this.accountsTrigger$.pipe(mergeMap(() => of('')))
    );
    
    return merged$.pipe(
      switchMap(term => {
        const apartmentId = this.searchForm.get('apartmentId')?.value;
        console.log('getAccounts: apartmentId =', apartmentId, 'term =', term);
        
        if (!apartmentId) {
          console.log('getAccounts: нет ID квартиры');
          return of([]);
        }
        
        if (term.length < 1) {
          console.log('getAccounts: загрузка всех счетов для квартиры');
          return this.apiService.getAccounts(apartmentId).pipe(
            tap(response => console.log('Счета (все):', response)),
            mergeMap((response: any) => of(response.data || []))
          );
        }
        
        console.log('getAccounts: поиск по термину:', term);
        return this.apiService.searchAccountByNumber(term).pipe(
          tap(response => console.log('Счета (поиск):', response)),
          mergeMap((response: any) => {
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
  localityFormatter = (result: Locality) => {
    console.log('localityFormatter:', result);
    return typeof result === 'string' ? result : result.name;
  };
  
  streetFormatter = (result: Street) => {
    console.log('streetFormatter:', result);
    return typeof result === 'string' ? result : result.name;
  };
  
  houseFormatter = (result: House) => {
    console.log('houseFormatter:', result);
    return typeof result === 'string' ? result : result.number;
  };
  
  apartmentFormatter = (result: Apartment) => {
    console.log('apartmentFormatter:', result);
    return typeof result === 'string' ? result : result.number;
  };
  
  accountFormatter = (result: Account) => {
    console.log('accountFormatter:', result);
    if (typeof result === 'string') {
      return result;
    }
    if (result && result.accountNumber) {
      return result.accountNumber;
    }
    return '';
  };

  onSubmit(): void {
    if (this.accountId) {
      console.log('Form submitted:', this.searchForm.value);
      // Здесь будет логика поиска/отображения результатов
    }
  }
}
