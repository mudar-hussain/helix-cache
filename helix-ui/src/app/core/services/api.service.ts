import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, Observable, throwError } from "rxjs";
import { environment } from "../../../environments/environment";


@Injectable({
  providedIn: 'root'
})
export class ApiService {

    constructor(private http: HttpClient) {}

    public withFallback<T>(
        method: 'get' | 'put' | 'delete' | 'post',
        path: string,
        options: {
            body?: any;
            params?: HttpParams;
            headers?: HttpHeaders | { [header: string]: string | string[] };
        } = {}
    ): Observable<T> {

        const request = (baseUrl: string): Observable<T> => {
            switch (method) {
                case 'get':
                    return this.http.get<T>(`${baseUrl}${path}`, options);

                case 'put':
                    return this.http.put<T>(
                        `${baseUrl}${path}`,
                        options.body ?? null,
                        options
                    );

                case 'post':
                    return this.http.post<T>(
                        `${baseUrl}${path}`,
                        options.body ?? null,
                        options
                    );

                case 'delete':
                    return this.http.delete<T>(`${baseUrl}${path}`, options);
            }
        };

        const shouldFallback = (error: HttpErrorResponse): boolean => {
            console.log(error);
            return (
            error.status === 0 ||     // Network / connection failure
            error.status === 502 ||   // Bad Gateway
            error.status === 503 ||   // Service unavailable
            error.status === 504      // Gateway timeout
            );
        };

        return environment.nodes.slice(1).reduce(
            (acc$, node) =>
                acc$.pipe(catchError((error: HttpErrorResponse) => {
                    if(!shouldFallback(error)) {
                        return throwError(() => error);
                    }
                    return request(node.baseUrl);
                })
            ),
            request(environment.nodes[0].baseUrl)
        );
    }

}