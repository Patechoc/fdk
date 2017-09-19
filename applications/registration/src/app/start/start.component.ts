import {Component, OnInit} from "@angular/core";
import {CatalogService} from "../catalog/catalog.service";
import {Catalog} from "../catalog/catalog";
import {Router} from "@angular/router";
import {AuthenticationService} from "../security/authentication.service";

@Component({
  selector: 'app-start',
  templateUrl: './start.component.html',
  styleUrls: ['./start.component.css']
})
export class StartComponent implements OnInit {
    catalogs: Catalog[] = [];
    language: string;
    selectedCatalog: Catalog;
    model: any = {};
    errorGettingCatalogs: boolean = false;



  constructor(private _catalogService: CatalogService,
              private router: Router,
              private authenticationService: AuthenticationService) {
  }

  ngOnInit() {
    this.language = 'nb';
    this._catalogService.getAll()
      .then(catalogs => this.catalogs = catalogs)
      .catch(err => {this.errorGettingCatalogs = true})
  }

  selectCatalog(catalog) {
    this.router.navigate(['/catalogs', catalog.id])
  }

}
