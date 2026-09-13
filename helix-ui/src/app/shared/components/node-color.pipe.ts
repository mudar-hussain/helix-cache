import { Pipe, PipeTransform } from "@angular/core";
import { environment } from "../../../environments/environment";


@Pipe({ name: 'nodeColor' , standalone: true , pure: true })
export class NodeColorPipe implements PipeTransform {
    transform(nodeId: string): string {
        return environment.nodes.find(node => node.id === nodeId)?.color || '#607d8b';
    }
}