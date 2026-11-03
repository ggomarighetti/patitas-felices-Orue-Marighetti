package com.patitas.turno.adapter.out.feign;

import com.patitas.turno.domain.exception.RemoteServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeignResilienceTest {

    @Test
    void transientFailuresAreRetriedAndOneLogicalSuccessIsRecorded() {
        runner().run(context -> {
            MascotaFeignClient client = context.getBean(MascotaFeignClient.class);
            MascotaFeignAdapter adapter = context.getBean(MascotaFeignAdapter.class);
            assertTrue(AopUtils.isAopProxy(adapter));
            when(client.findById(7L)).thenThrow(remoteError(503)).thenThrow(remoteError(500))
                    .thenReturn(mascota());

            assertTrue(adapter.existsById(7L));

            verify(client, times(3)).findById(7L);
            CircuitBreaker circuit = context.getBean(CircuitBreakerRegistry.class).circuitBreaker("mascotaService");
            assertEquals(1, circuit.getMetrics().getNumberOfSuccessfulCalls());
            assertEquals(0, circuit.getMetrics().getNumberOfFailedCalls());
        });
    }

    @Test
    void exhaustedRetriesOpenOnlyTheFailingCircuitAndPreserveTheOriginalCause() {
        runner().run(context -> {
            MascotaFeignClient client = context.getBean(MascotaFeignClient.class);
            MascotaFeignAdapter adapter = context.getBean(MascotaFeignAdapter.class);
            FeignException original = remoteError(503);
            when(client.findById(7L)).thenThrow(original);

            for (int call = 0; call < 5; call++) {
                RemoteServiceUnavailableException fallback = assertThrows(
                        RemoteServiceUnavailableException.class, () -> adapter.existsById(7L));
                assertTrue(fallback.getMessage().contains("servicio de mascotas"));
                assertSame(original, rootCause(fallback));
            }

            CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);
            assertEquals(CircuitBreaker.State.OPEN, registry.circuitBreaker("mascotaService").getState());
            assertEquals(CircuitBreaker.State.CLOSED, registry.circuitBreaker("veterinarioService").getState());
            assertTimeout(Duration.ofSeconds(1), () -> assertThrows(RemoteServiceUnavailableException.class,
                    () -> adapter.existsById(7L)));
            verify(client, times(15)).findById(7L);
        });
    }

    @Test
    void threeSuccessfulProbesCloseTheCircuitAgain() {
        runner().run(context -> {
            MascotaFeignClient client = context.getBean(MascotaFeignClient.class);
            MascotaFeignAdapter adapter = context.getBean(MascotaFeignAdapter.class);
            CircuitBreaker circuit = context.getBean(CircuitBreakerRegistry.class).circuitBreaker("mascotaService");
            circuit.transitionToOpenState();
            circuit.transitionToHalfOpenState();
            when(client.findById(7L)).thenReturn(mascota());

            assertTrue(adapter.existsById(7L));
            assertEquals(CircuitBreaker.State.HALF_OPEN, circuit.getState());
            assertTrue(adapter.existsById(7L));
            assertEquals(CircuitBreaker.State.HALF_OPEN, circuit.getState());
            assertTrue(adapter.existsById(7L));
            assertEquals(CircuitBreaker.State.CLOSED, circuit.getState());
            verify(client, times(3)).findById(7L);
        });
    }

    @Test
    void businessNotFoundIsNotRetriedAndDoesNotOpenTheCircuit() {
        runner().run(context -> {
            MascotaFeignClient client = context.getBean(MascotaFeignClient.class);
            MascotaFeignAdapter adapter = context.getBean(MascotaFeignAdapter.class);
            when(client.findById(7L)).thenThrow(remoteError(404));

            for (int call = 0; call < 6; call++) {
                assertFalse(adapter.existsById(7L));
            }

            verify(client, times(6)).findById(7L);
            CircuitBreaker circuit = context.getBean(CircuitBreakerRegistry.class).circuitBreaker("mascotaService");
            assertEquals(CircuitBreaker.State.CLOSED, circuit.getState());
            assertEquals(0, circuit.getMetrics().getNumberOfFailedCalls());
        });
    }

    @Test
    void nonTransientClientErrorUsesFallbackWithoutRetrying() {
        runner().run(context -> {
            MascotaFeignClient client = context.getBean(MascotaFeignClient.class);
            when(client.findById(7L)).thenThrow(remoteError(403));

            assertThrows(RemoteServiceUnavailableException.class,
                    () -> context.getBean(MascotaFeignAdapter.class).existsById(7L));

            verify(client).findById(7L);
        });
    }

    @Test
    void veterinarianAdapterHasAnActiveRetryAndFallback() {
        runner().run(context -> {
            VeterinarioFeignClient client = context.getBean(VeterinarioFeignClient.class);
            VeterinarioFeignAdapter adapter = context.getBean(VeterinarioFeignAdapter.class);
            FeignException original = remoteError(502);
            when(client.findById(7L)).thenThrow(original);
            assertTrue(AopUtils.isAopProxy(adapter));

            RemoteServiceUnavailableException fallback = assertThrows(
                    RemoteServiceUnavailableException.class, () -> adapter.existsById(7L));

            assertTrue(fallback.getMessage().contains("servicio de veterinarios"));
            assertSame(original, rootCause(fallback));
            verify(client, times(3)).findById(7L);
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class,
                        CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class))
                .withBean(MascotaFeignClient.class, () -> mock(MascotaFeignClient.class))
                .withBean(VeterinarioFeignClient.class, () -> mock(VeterinarioFeignClient.class))
                .withBean(MascotaFeignAdapter.class)
                .withBean(VeterinarioFeignAdapter.class)
                .withPropertyValues(
                        "resilience4j.circuitbreaker.circuitBreakerAspectOrder=1",
                        "resilience4j.retry.retryAspectOrder=2",
                        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=10",
                        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5",
                        "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50",
                        "resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=10s",
                        "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState=3",
                        "resilience4j.circuitbreaker.instances.mascotaService.baseConfig=default",
                        "resilience4j.circuitbreaker.instances.veterinarioService.baseConfig=default",
                        "resilience4j.retry.configs.default.maxAttempts=3",
                        "resilience4j.retry.configs.default.waitDuration=1ms",
                        "resilience4j.retry.configs.default.enableExponentialBackoff=true",
                        "resilience4j.retry.configs.default.exponentialBackoffMultiplier=2",
                        "resilience4j.retry.configs.default.retryExceptionPredicate=" + RemoteCallRetryPredicate.class.getName(),
                        "resilience4j.retry.instances.mascotaService.baseConfig=default",
                        "resilience4j.retry.instances.veterinarioService.baseConfig=default");
    }

    private MascotaResponse mascota() {
        MascotaResponse response = new MascotaResponse();
        response.setId(7L);
        return response;
    }

    private Throwable rootCause(Throwable error) {
        return error.getCause() == null ? error : rootCause(error.getCause());
    }

    private FeignException remoteError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://test/resource/7",
                Map.of(), null, StandardCharsets.UTF_8);
        return FeignException.errorStatus("findById", Response.builder().status(status)
                .reason("Respuesta de prueba").headers(Map.of()).request(request).build());
    }
}
