import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { environment } from "../../../../../environments/environment";

@Component({
    selector: 'app-topbar',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './topbar.component.html',
    styleUrl: './topbar.component.css',
})
export class TopbarComponent {
    
}