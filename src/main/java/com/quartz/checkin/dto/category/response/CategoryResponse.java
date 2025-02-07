package com.quartz.checkin.dto.category.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private Long firstCategoryId;
    private String firstCategoryName;
    private String alias;
    private String contentGuide;
    private List<SecondCategoryResponse> secondCategories;
}
