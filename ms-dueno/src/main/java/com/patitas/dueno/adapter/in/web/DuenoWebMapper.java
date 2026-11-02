package com.patitas.dueno.adapter.in.web;

import com.patitas.dueno.adapter.in.web.dto.DuenoRequest;
import com.patitas.dueno.adapter.in.web.dto.DuenoResponse;
import com.patitas.dueno.domain.model.Dueno;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DuenoWebMapper {

    @Mapping(target = "id", ignore = true)
    Dueno toDomain(DuenoRequest request);

    DuenoResponse toResponse(Dueno dueno);
}
