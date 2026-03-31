import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-statistics',
  template: `
    <div class="card">
      <div class="card-header">
        📊 Статистика базы данных
      </div>
      <div class="card-body">
        <div class="row">
          <div class="col-md-4 mb-3" *ngFor="let stat of statistics | keyvalue">
            <div class="statistics-card text-center">
              <h5>{{ getLabel(stat.key) }}</h5>
              <div class="value">{{ stat.value | number }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class StatisticsComponent implements OnInit {
  statistics: Record<string, number> = {};

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.apiService.getStatistics().subscribe(response => {
      if (response.data) {
        this.statistics = response.data;
      }
    });
  }

  getLabel(key: string): string {
    const labels: Record<string, string> = {
      localities: 'Населенные пункты',
      streets: 'Улицы',
      houses: 'Дома',
      apartments: 'Квартиры',
      accounts: 'Лицевые счета',
      billingPeriods: 'Периоды начислений'
    };
    return labels[key] || key;
  }
}
