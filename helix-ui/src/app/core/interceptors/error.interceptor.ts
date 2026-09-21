import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { catchError, throwError } from "rxjs";


export const errorInterceptor: HttpInterceptorFn = (request, next) => 
    next(request).pipe(
        catchError((error: HttpErrorResponse) => {
            console.error('HTTP Error:', error);

      // Preserve the original HttpErrorResponse
      return throwError(() => error);
        })
    );