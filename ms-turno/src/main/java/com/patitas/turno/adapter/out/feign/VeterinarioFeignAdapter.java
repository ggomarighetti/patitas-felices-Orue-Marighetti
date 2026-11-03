package com.patitas.turno.adapter.out.feign;

import com.patitas.turno.domain.exception.RemoteServiceUnavailableException;
import com.patitas.turno.domain.port.out.VeterinarioServicePort;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VeterinarioFeignAdapter implements VeterinarioServicePort {

    private final VeterinarioFeignClient client;

    @Override
    @CircuitBreaker(name = "veterinarioService", fallbackMethod = "fallbackVeterinario")
    @Retry(name = "veterinarioService", fallbackMethod = "fallbackVeterinario")
    public boolean existsById(Long id) {
        try {
            VeterinarioResponse response = client.findById(id);
            if (response == null || !id.equals(response.getId())) {
                throw new RemoteServiceUnavailableException("Respuesta inválida del servicio remoto: ms-veterinario");
            }
            return true;
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            throw new RemoteServiceUnavailableException("Servicio remoto no disponible: ms-veterinario", exception);
        }
    }

    private boolean fallbackVeterinario(Long id, Throwable cause) {
        throw new RemoteServiceUnavailableException(
                "El servicio de veterinarios no está disponible. Intentá nuevamente más tarde.", cause);
    }
}
