package com.patitas.turno.adapter.out.feign;

import com.patitas.turno.domain.exception.RemoteServiceUnavailableException;
import com.patitas.turno.domain.port.out.MascotaServicePort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MascotaFeignAdapter implements MascotaServicePort {

    private final MascotaFeignClient client;

    @Override
    public boolean existsById(Long id) {
        try {
            MascotaResponse response = client.findById(id);
            if (response == null || !id.equals(response.getId())) {
                throw new RemoteServiceUnavailableException("Respuesta inválida del servicio remoto: ms-mascota");
            }
            return true;
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            throw new RemoteServiceUnavailableException("Servicio remoto no disponible: ms-mascota", exception);
        }
    }
}
