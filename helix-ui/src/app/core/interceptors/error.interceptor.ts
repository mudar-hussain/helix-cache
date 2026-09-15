import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { catchError, throwError } from "rxjs";


export const errorInterceptor: HttpInterceptorFn = (request, next) => 
    next(request).pipe(
        catchError((error: HttpErrorResponse) => {
            let errorMessage = 'An unknown error occurred!';
            if (error.error instanceof ErrorEvent) {
                // Client-side error
                errorMessage = `Error: ${error.error.message}`;
            } else {
                // Server-side error
                errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
            }
            console.error(errorMessage);
            return throwError(() => new Error(errorMessage));
        })
    );