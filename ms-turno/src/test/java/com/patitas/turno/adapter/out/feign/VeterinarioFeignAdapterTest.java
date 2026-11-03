package com.patitas.turno.adapter.out.feign;

import com.patitas.turno.domain.exception.RemoteServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VeterinarioFeignAdapterTest {

    private VeterinarioFeignClient client;
    private VeterinarioFeignAdapter adapter;

    @BeforeEach
    void setUp() {
        client = mock(VeterinarioFeignClient.class);
        adapter = new VeterinarioFeignAdapter(client);
    }

    @Test
    void existingResourceReturnsTrue() {
        VeterinarioResponse response = new VeterinarioResponse();
        response.setId(7L);
        when(client.findById(7L)).thenReturn(response);

        assertTrue(adapter.existsById(7L));
    }

    @Test
    void onlyRemoteNotFoundReturnsFalse() {
        when(client.findById(7L)).thenThrow(remoteError(404));

        assertFalse(adapter.existsById(7L));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 500, 502, 503})
    void otherHttpErrorsPreserveTheCauseAndReportUnavailability(int status) {
        FeignException original = remoteError(status);
        when(client.findById(7L)).thenThrow(original);

        RemoteServiceUnavailableException error = assertThrows(
                RemoteServiceUnavailableException.class, () -> adapter.existsById(7L));

        assertSame(original, error.getCause());
    }

    @Test
    void emptyResponseDoesNotValidateTheResource() {
        assertThrows(RemoteServiceUnavailableException.class, () -> adapter.existsById(7L));
    }

    @Test
    void responseForAnotherIdDoesNotValidateTheResource() {
        VeterinarioResponse response = new VeterinarioResponse();
        response.setId(99L);
        when(client.findById(7L)).thenReturn(response);

        assertThrows(RemoteServiceUnavailableException.class, () -> adapter.existsById(7L));
    }

    private FeignException remoteError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://test/resource/7",
                Map.of(), null, StandardCharsets.UTF_8);
        return FeignException.errorStatus("findById", Response.builder().status(status)
                .reason("Respuesta de prueba").headers(Map.of()).request(request).build());
    }
}
