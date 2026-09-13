import { Directive, ElementRef, Input, OnChanges } from "@angular/core";

@Directive({
    selector: '[autoScroll]',
    standalone: true,
})
export class AutoScrollDirective implements OnChanges {
    @Input('appAutoScroll') trigger: unknown;

    ngOnChanges() {
        this.scrollToBottom();
    }
    constructor(private elementRef: ElementRef<HTMLElement>) {}
    scrollToBottom(): void {
        const element = this.elementRef.nativeElement;
        const atBottom = element.scrollHeight - element.scrollTop - element.clientHeight < 80;
        if (atBottom) {
            element.scrollTop = element.scrollHeight;
        }
    }    
}