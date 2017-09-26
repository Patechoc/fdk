import {BrowserModule} from "@angular/platform-browser";
import {LOCALE_ID, NgModule, ErrorHandler} from "@angular/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {HttpModule} from "@angular/http";
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";
import {AppComponent} from "./app.component";
import {CatalogComponent} from "./catalog/catalog.component";
import {routes} from "./routes/app.routes";
import {RouterModule} from "@angular/router";
import {AlertModule} from "ng2-bootstrap";
import {DatasetComponent} from "./dataset/dataset.component";
import {StartComponent} from "./start/start.component";
import {CatalogService} from "./catalog/catalog.service";
import {DatasetService} from "./dataset/dataset.service";
import {ThemesService} from "./dataset/themes.service";
import {CodesService} from "./dataset/codes.service";
import {BootstrapModalModule} from "ng2-bootstrap-modal";
import {ConfirmComponent} from "./confirm/confirm.component";
import {AuthGuard} from "./security/auth.guard";
import {AuthenticationService} from "./security/authentication.service";
import {RlTagInputModule} from "angular2-tag-input";
import {TooltipModule} from 'ngx-bootstrap';
import {Ng2Webstorage} from 'ngx-webstorage';


import {SelectModule} from "ng-select";

import {DistributionFormComponent} from "./dataset/distribution/distribution.component";
import {DistributionListComponent} from "./dataset/distribution/distribution-list.component";
import {TemporalListComponent} from "./dataset/temporal/temporal-list.component";
import {TemporalFormComponent} from "./dataset/temporal/temporal.component";
import {ContactComponent} from "./dataset/contact/contact.component";
import {QualityComponent} from "./dataset/quality/quality.component";
import {AccessRightsComponent} from "./dataset/accessRights/accessRights.component";
import {TypeComponent} from "./dataset/type/type.component";
import {InformationComponent} from "./dataset/information/information.component";
import {TemaComponent} from "./dataset/tema/tema.component";
import {MyDatePickerModule} from 'mydatepicker';
import {SpatialComponent} from "./dataset/spatial/spatial.component";
import {HelpText} from "./dataset/helptext/helptext.component";
import {BsDropdownModule} from 'ngx-bootstrap/dropdown';
import {ModalComponent} from "./modal/modal.component";
import {restrictedPursuantToLegalBasisListComponent} from "./dataset/restrictedPursuantToLegalBasis/restrictedPursuantToLegalBasis-list.component";
import {restrictedPursuantToLegalBasisFormComponent} from "./dataset/restrictedPursuantToLegalBasis/restrictedPursuantToLegalBasis.component";
import {BasisForProcessingListComponent} from "./dataset/legalBasisForProcessing/legalBasisForProcessing-list.component";
import {BasisForProcessingFormComponent} from "./dataset/legalBasisForProcessing/legalBasisForProcessing.component";
import { TagInputModule } from 'ngx-chips';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'; // this is needed!

import {GlobalErrorHandler} from './GlobalErrorHandler';

@NgModule({
  declarations: [
    AppComponent,
    CatalogComponent,
    DatasetComponent,
    StartComponent,
    ConfirmComponent,
    DistributionListComponent,
    DistributionFormComponent,
    TemporalFormComponent,
    TemporalListComponent,
    ContactComponent,
    QualityComponent,
    AccessRightsComponent,
    TypeComponent,
    InformationComponent,
    TemaComponent,
    SpatialComponent,
    HelpText,
    ModalComponent,
    restrictedPursuantToLegalBasisListComponent,
    restrictedPursuantToLegalBasisFormComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    SelectModule,
    HttpModule,
    NgbModule.forRoot(),
    AlertModule.forRoot(),
    RouterModule.forRoot(routes),
    BootstrapModalModule,
    RlTagInputModule,
    MyDatePickerModule,
    BsDropdownModule.forRoot(),
    TooltipModule.forRoot(),
    Ng2Webstorage.forRoot({prefix: 'dcat-registration-gui', separator: '.', caseSensitive: true}),
    TagInputModule,
    BrowserAnimationsModule
  ],
  entryComponents: [
    ConfirmComponent
  ],
  providers: [
    CatalogService,
    DatasetService,
    CodesService,
    ThemesService,
    AuthGuard,
    AuthenticationService,
    {
      provide: LOCALE_ID,
      useValue: "no-NO"
    },
    {
      provide: ErrorHandler,
      useClass: GlobalErrorHandler
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
