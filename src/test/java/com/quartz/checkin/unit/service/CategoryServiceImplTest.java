package com.quartz.checkin.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.category.request.*;
import com.quartz.checkin.dto.category.response.*;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.CategoryServiceImpl;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category firstCategory;
    private Category secondCategory;

    @BeforeEach
    void setUp() {
        firstCategory = new Category(null, "DevOps", "DEV", "Development and Operations Guide");
        secondCategory = new Category(firstCategory, "Infrastructure", "INFR", null);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("모든 카테고리 조회 성공")
    void getAllCategoriesSuccess() {
        when(categoryRepository.findAllCategoriesWithSecondCategories())
                .thenReturn(List.of());

        List<CategoryResponse> response = categoryService.getAllCategories(1L);
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    @DisplayName("1차 카테고리 생성 성공")
    void createFirstCategorySuccess() {
        FirstCategoryCreateRequest request = new FirstCategoryCreateRequest();

        when(categoryRepository.existsByNameAndParentIsNull(request.getName())).thenReturn(false);
        when(categoryRepository.existsByAlias(request.getAlias())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(firstCategory);

        FirstCategoryCreateResponse response = categoryService.createFirstCategory(1L, request);

        assertNotNull(response);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("1차 카테고리 생성 실패 - 중복 이름")
    void createFirstCategoryFailDuplicateName() {
        FirstCategoryCreateRequest request = new FirstCategoryCreateRequest();

        when(categoryRepository.existsByNameAndParentIsNull(request.getName())).thenReturn(true);

        ApiException thrown = assertThrows(ApiException.class, () -> categoryService.createFirstCategory(1L, request));
        assertEquals(ErrorCode.DUPLICATE_CATEGORY_FIRST, thrown.getErrorCode());
    }

    @Test
    @DisplayName("1차 카테고리 삭제 성공")
    void deleteFirstCategorySuccess() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(firstCategory));
        when(categoryRepository.existsByParent(firstCategory)).thenReturn(false);
        when(ticketRepository.existsByFirstCategory(firstCategory)).thenReturn(false);

        assertDoesNotThrow(() -> categoryService.deleteFirstCategory(1L, 1L));
        verify(categoryRepository, times(1)).delete(firstCategory);
    }

    @Test
    @DisplayName("1차 카테고리 삭제 실패 - 하위 카테고리 존재")
    void deleteFirstCategory_Fail_HasSubcategories() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(firstCategory));
        when(categoryRepository.existsByParent(firstCategory)).thenReturn(true);

        ApiException thrown = assertThrows(ApiException.class, () -> categoryService.deleteFirstCategory(1L, 1L));
        assertEquals(ErrorCode.CATEGORY_HAS_SUBCATEGORIES, thrown.getErrorCode());
    }

    @Test
    @DisplayName("2차 카테고리 생성 성공")
    void createSecondCategorySuccess() {
        SecondCategoryCreateRequest request = new SecondCategoryCreateRequest();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(firstCategory));
        when(categoryRepository.existsByNameAndParent(request.getName(), firstCategory)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(secondCategory);

        SecondCategoryCreateResponse response = categoryService.createSecondCategory(1L, 1L, request);

        assertNotNull(response);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("2차 카테고리 생성 실패 - 중복 이름")
    void createSecondCategoryFailDuplicateName() {
        SecondCategoryCreateRequest request = new SecondCategoryCreateRequest();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(firstCategory));
        when(categoryRepository.existsByNameAndParent(request.getName(), firstCategory)).thenReturn(true);

        ApiException thrown = assertThrows(ApiException.class, () -> categoryService.createSecondCategory(1L, 1L, request));
        assertEquals(ErrorCode.DUPLICATE_CATEGORY_SECOND, thrown.getErrorCode());
    }

    @Test
    @DisplayName("2차 카테고리 삭제 성공")
    void deleteSecondCategorySuccess() throws Exception {
        Long firstCategoryId = 1L;
        Long secondCategoryId = 2L;

        setField(firstCategory, "id", firstCategoryId);
        setField(secondCategory, "id", secondCategoryId);
        setField(secondCategory, "parent", firstCategory);

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(secondCategory));
        when(ticketRepository.existsBySecondCategory(secondCategory)).thenReturn(false);

        assertDoesNotThrow(() -> categoryService.deleteSecondCategory(1L, firstCategoryId, secondCategoryId));
        verify(categoryRepository, times(1)).delete(secondCategory);
    }

    @Test
    void deleteSecondCategoryFailInUse() throws Exception {
        Long firstCategoryId = 1L;
        Long secondCategoryId = 2L;

        setField(firstCategory, "id", firstCategoryId);
        setField(secondCategory, "id", secondCategoryId);
        setField(secondCategory, "parent", firstCategory);

        when(categoryRepository.findById(secondCategoryId)).thenReturn(Optional.of(secondCategory));
        when(ticketRepository.existsBySecondCategory(secondCategory)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> categoryService.deleteSecondCategory(1L, firstCategoryId, secondCategoryId)
        );

        assertEquals(ErrorCode.SECOND_CATEGORY_IN_USE, exception.getErrorCode());
    }


    @Test
    @DisplayName("1차 카테고리 조회 성공")
    void getFirstCategoryOrThrowSuccess() {
        when(categoryRepository.findByNameAndParentIsNull("DevOps")).thenReturn(Optional.of(firstCategory));

        Category result = categoryService.getFirstCategoryOrThrow("DevOps");

        assertNotNull(result);
        assertEquals("DevOps", result.getName());
    }

    @Test
    @DisplayName("1차 카테고리 조회 실패")
    void getFirstCategoryOrThrowFailNotFound() {
        when(categoryRepository.findByNameAndParentIsNull("DevOps")).thenReturn(Optional.empty());

        ApiException thrown = assertThrows(ApiException.class, () -> categoryService.getFirstCategoryOrThrow("DevOps"));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND_FIRST, thrown.getErrorCode());
    }

    @Test
    @DisplayName("2차 카테고리 조회 성공")
    void getSecondCategoryOrThrowSuccess() {
        when(categoryRepository.findByNameAndParent("Infrastructure", firstCategory)).thenReturn(Optional.of(secondCategory));

        Category result = categoryService.getSecondCategoryOrThrow("Infrastructure", firstCategory);

        assertNotNull(result);
        assertEquals("Infrastructure", result.getName());
    }

    @Test
    @DisplayName("2차 카테고리 조회 실패")
    void getSecondCategoryOrThrowFailNotFound() {
        when(categoryRepository.findByNameAndParent("Infrastructure", firstCategory)).thenReturn(Optional.empty());

        ApiException thrown = assertThrows(ApiException.class, () -> categoryService.getSecondCategoryOrThrow("Infrastructure", firstCategory));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND_SECOND, thrown.getErrorCode());
    }
}