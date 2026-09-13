import { Component, Input } from "@angular/core";
import { NodeStatus } from "../../../core/enums/helix.enum";


@Component({
    selector: 'app-status-badge',
    templateUrl: './status-badge.component.html',
    styleUrl: './status-badge.component.css'
})
export class StatusBadgeComponent {
    @Input({ required: true }) status!: NodeStatus;

    get label(): string {
        switch (this.status) {
            case NodeStatus.UP:
                return 'UP';
            case NodeStatus.DOWN:
                return 'DOWN';
            case NodeStatus.SUSPECT:
                return 'SUSPECT';
            default:
                return '';
        }
    }
}