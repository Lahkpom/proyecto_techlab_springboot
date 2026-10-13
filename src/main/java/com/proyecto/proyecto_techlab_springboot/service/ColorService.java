package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.color.ColorCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.color.ColorResponse;
import com.proyecto.proyecto_techlab_springboot.dto.color.ColorUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.common.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.exception.color.ColorAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.exception.color.ColorNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Color;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.repository.IColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColorService {

    private final IColorRepository iColorRepository;

    /**
     * GET /colores                     : Por defecto devuelve solo los activos (vista pública / cliente).
     * GET /colores?includeInactive=true: Devuelve activos e inactivos (vista administrativa).
     * @param includeInactive solo incluir true si corresponde a un panel administrativo
     * @return listado con activos e inactivos o solo con activos dependiendo del includeInactive
     */
    @Transactional(readOnly = true)
    public List<ColorResponse> getColors(boolean includeInactive) {
        List<Color> colors = includeInactive
                ? iColorRepository.findAll()
                : iColorRepository.findAllByRowStatus(RowStatus.ACTIVE);

        return colors.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public ColorResponse getColorById(Integer id) {
        return mapToResponse(findColorByIdOrThrow(id));
    }

    @Transactional
    public ColorResponse createColor(ColorCreateRequest request) {
        String name = request.name().trim();
        String desc = request.desc().trim();
        String code = request.code().toUpperCase().trim();

        if (iColorRepository.existsByColorNameIgnoreCase(name))
            throw new ColorAlreadyExistsException("{ name: %s }".formatted(name));

        if (iColorRepository.existsByColorCodeIgnoreCase(code))
            throw new ColorAlreadyExistsException("{ code: %s }".formatted(code));

        Color color = Color.builder()
                .colorName(name)
                .colorDesc(desc)
                .colorCode(code)
                .build();

        Color savedColor = iColorRepository.save(color);

        log.info("Color creado exitosamente { ID: {}, nombre: {}, código: {} }", savedColor.getColorId(), savedColor.getColorName(), savedColor.getColorCode());
        return mapToResponse(savedColor);
    }

    @Transactional
    public ColorResponse updateColor(Integer id, ColorUpdateRequest request) {
        Color color = findColorByIdOrThrow(id);

        String name = request.name().trim();
        String desc = request.desc().trim();
        String code = request.code().toUpperCase().trim();

        if (iColorRepository.existsByColorNameIgnoreCaseAndColorIdNot(name, id))
            throw new ColorAlreadyExistsException("{ name: %s }".formatted(name));

        if (iColorRepository.existsByColorCodeIgnoreCaseAndColorIdNot(code, id))
            throw new ColorAlreadyExistsException("{ code: %s }".formatted(code));

        color.setColorName(name);
        color.setColorDesc(desc);
        color.setColorCode(code);

        log.info("Color modificado exitosamente { ID: {}, nombre: {}, código: {} }", color.getColorId(), color.getColorName(), color.getColorCode());
        return mapToResponse(color);
    }

    @Transactional
    public ColorResponse updateColorStatus(Integer id, StatusUpdateRequest request) {
        Color color = findColorByIdOrThrow(id);

        color.setRowStatus(request.rowStatus());

        log.info("Estado de Color modificado exitosamente { ID: {}, estado: {} }", color.getColorId(), color.getRowStatus());

        return mapToResponse(color);
    }

    public Color findColorByIdOrThrow(Integer id) {
        return iColorRepository.findById(id)
                .orElseThrow(() -> new ColorNotFoundException("{ ID: %s }".formatted(id)));
    }

    private ColorResponse mapToResponse(Color color) {
        return new ColorResponse(
                color.getColorId(),
                color.getColorName(),
                color.getColorDesc(),
                color.getColorCode(),
                color.getRowStatus(),
                color.getCreatedAt(),
                color.getUpdatedAt()
        );
    }
}