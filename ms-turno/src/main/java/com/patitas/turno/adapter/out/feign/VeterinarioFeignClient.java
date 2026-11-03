package com.patitas.turno.adapter.out.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-veterinario")
public interface VeterinarioFeignClient {

    @GetMapping(value = "/api/veterinarios/{id}", produces = "application/json")
    VeterinarioResponse findById(@PathVariable("id") Long id);
}
