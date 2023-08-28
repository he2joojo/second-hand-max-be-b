package kr.codesquad.secondhand.presentation.dto.category;

import java.util.List;
import java.util.stream.Collectors;
import kr.codesquad.secondhand.domain.category.Category;
import lombok.Getter;

@Getter
public class CategoryListResponse {

    private final List<CategoryResponse> categories;

    public CategoryListResponse(List<CategoryResponse> categories) {
        this.categories = categories;
    }

    public static CategoryListResponse toResponse(List<Category> categories) {
        List<CategoryResponse> list = categories.stream()
                .map(CategoryResponse::toResponse)
                .collect(Collectors.toList());
        return new CategoryListResponse(list);
    }
}