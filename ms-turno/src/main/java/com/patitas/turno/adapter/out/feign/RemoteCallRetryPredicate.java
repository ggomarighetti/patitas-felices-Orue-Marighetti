package com.patitas.turno.adapter.out.feign;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.function.Predicate;

public class RemoteCallRetryPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable error) {
        if (error instanceof CallNotPermittedException) {
            return false;
        }
        if (error instanceof FeignException remoteError) {
            return remoteError.status() < 0 || remoteError.status() == 429 || remoteError.status() >= 500;
        }
        return error.getCause() != null && error.getCause() != error && test(error.getCause());
    }
}
