import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { TopbarComponent } from "./components/topbar/topbar.component";
import { NodeControlComponent } from "../node-control/note-control.component";
import { CacheOpsComponent } from "../cache-ops/cache-ops.component";


@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [RouterOutlet, TopbarComponent, NodeControlComponent, CacheOpsComponent],
    templateUrl: './dashboard.component.html',
    styleUrl:'./dashboard.component.css',
})
export class DashboardComponent {}