import { Component, Input } from "@angular/core";


@Component({
    selector: 'app-stat-card',
    templateUrl: './stat-card.component.html',
    styleUrl: './stat-card.component.css'
})
export class StatCardComponent {
    @Input({ required: true }) value!: string;
    @Input({ required: true }) label!: string;
    @Input() sub?: string;
}