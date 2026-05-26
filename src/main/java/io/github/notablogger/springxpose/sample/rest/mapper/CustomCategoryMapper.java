package io.github.notablogger.springxpose.sample.rest.mapper;

import io.github.notablogger.springxpose.sample.rest.entity.Category;
import io.github.notablogger.springxpose.sample.rest.entity.generated.CategoryDto;
import io.github.notablogger.springxpose.sample.rest.entity.generated.CategoryMapper;
import io.github.notablogger.springxpose.sample.rest.entity.generated.CategoryRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Custom mapper for {@link Category} — demonstrates {@code @ExposeEntity(customMapper = ...)}.
 *
 * <p>This bean implements the generated {@code CategoryMapper} interface and is picked up by
 * Spring instead of the MapStruct-generated implementation.
 *
 * <p>Custom transformation: upper-cases category names in all responses (demo of custom logic).
 */
@Component
public class CustomCategoryMapper implements CategoryMapper {

    @Override
    public CategoryDto toDto(Category entity) {
        if (entity == null) return null;
        CategoryDto dto = new CategoryDto();
        dto.setId(entity.getId());
        // Custom transformation: upper-case the name in responses
        dto.setName(entity.getName() != null ? entity.getName().toUpperCase() : null);
        dto.setDescription(entity.getDescription());
        return dto;
    }

    @Override
    public List<CategoryDto> toDtoList(List<Category> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }

    @Override
    public Category toEntity(CategoryRequestDto requestDto) {
        if (requestDto == null) return null;
        Category category = new Category();
        category.setName(requestDto.getName());
        category.setDescription(requestDto.getDescription());
        return category;
    }

    @Override
    public void updateEntity(CategoryRequestDto requestDto, Category entity) {
        if (requestDto == null) return;
        // Null-safe merge: only overwrite if the client sent a value
        if (requestDto.getName() != null) entity.setName(requestDto.getName());
        if (requestDto.getDescription() != null) entity.setDescription(requestDto.getDescription());
    }
}
