package com.patitas.turno.adapter.out.feign;

import com.patitas.turno.domain.exception.RemoteServiceUnavailableException;
import com.patitas.turno.domain.port.out.VeterinarioServicePort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VeterinarioFeignAdapter implements VeterinarioServicePort {

    private final VeterinarioFeignClient client;

    @Override
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
}
