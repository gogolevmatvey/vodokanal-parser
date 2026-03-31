import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImportComponent } from './import.component';
import { ImportRoutingModule } from './import-routing.module';

@NgModule({
  declarations: [
    ImportComponent
  ],
  imports: [
    CommonModule,
    ImportRoutingModule
  ]
})
export class ImportModule { }
