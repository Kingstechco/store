package com.securitease.store.service;

import com.securitease.store.dto.ProductDTO;
import com.securitease.store.dto.ProductRequest;
import com.securitease.store.entity.Product;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.mapper.ProductMapper;
import com.securitease.store.repository.ProductRepository;
import com.securitease.store.service.impl.ProductServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product phoneEntity;
    private Product tabletEntity;
    private Product savedEntity;
    private Product updatedEntity;

    private ProductDTO phoneDto;
    private ProductDTO tabletDto;
    private ProductDTO savedDto;
    private ProductDTO updatedDto;

    private ProductRequest createRequest;
    private ProductRequest updateRequest;

    @BeforeEach
    void setUp() {
        phoneEntity = new Product();
        phoneEntity.setId(1L);
        phoneEntity.setDescription("Phone");

        tabletEntity = new Product();
        tabletEntity.setId(2L);
        tabletEntity.setDescription("Tablet");

        savedEntity = new Product();
        savedEntity.setId(100L);
        savedEntity.setDescription("New");

        updatedEntity = new Product();
        updatedEntity.setId(1L);
        updatedEntity.setDescription("Updated");

        phoneDto = new ProductDTO();
        phoneDto.setId(1L);
        phoneDto.setDescription("Phone");

        tabletDto = new ProductDTO();
        tabletDto.setId(2L);
        tabletDto.setDescription("Tablet");

        savedDto = new ProductDTO();
        savedDto.setId(100L);
        savedDto.setDescription("New");

        updatedDto = new ProductDTO();
        updatedDto.setId(1L);
        updatedDto.setDescription("Updated");

        createRequest = new ProductRequest();
        createRequest.setDescription("New");

        updateRequest = new ProductRequest();
        updateRequest.setDescription("Updated");
    }

    // ------------------- getAllProducts -------------------

    @Test
    @DisplayName("getAllProducts returns mapped list")
    void getAllProducts_returnsMappedList() {
        List<Product> entityList = List.of(phoneEntity, tabletEntity);
        List<ProductDTO> dtoList = List.of(phoneDto, tabletDto);

        when(productRepository.findAll()).thenReturn(entityList);
        when(productMapper.productsToProductDTOs(entityList)).thenReturn(dtoList);

        List<ProductDTO> result = productService.getAllProducts();

        assertThat(result).containsExactly(phoneDto, tabletDto);
        verify(productRepository).findAll();
        verify(productMapper).productsToProductDTOs(entityList);
    }

    // ------------------- getProducts (paged) -------------------

    @Test
    @DisplayName("getProducts maps entities in page")
    void getProducts_mapsEntitiesInPage() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());
        List<Product> pageContent = List.of(phoneEntity, tabletEntity);
        Page<Product> productPage = new PageImpl<>(pageContent, pageable, 5);

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(productMapper.productToProductDTO(phoneEntity)).thenReturn(phoneDto);
        when(productMapper.productToProductDTO(tabletEntity)).thenReturn(tabletDto);

        Page<ProductDTO> resultPage = productService.getProducts(pageable);

        assertThat(resultPage.getTotalElements()).isEqualTo(5);
        assertThat(resultPage.getContent()).containsExactly(phoneDto, tabletDto);
        verify(productRepository).findAll(pageable);
        verify(productMapper).productToProductDTO(phoneEntity);
        verify(productMapper).productToProductDTO(tabletEntity);
    }

    // ------------------- getProductById -------------------

    @Test
    @DisplayName("getProductById returns Optional DTO when entity is present")
    void getProductById_returnsDtoWhenPresent() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(phoneEntity));
        when(productMapper.productToProductDTO(phoneEntity)).thenReturn(phoneDto);

        Optional<ProductDTO> result = productService.getProductById(1L);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(phoneDto);
        verify(productRepository).findById(1L);
        verify(productMapper).productToProductDTO(phoneEntity);
    }

    @Test
    @DisplayName("getProductById returns empty Optional when entity is missing")
    void getProductById_returnsEmptyWhenMissing() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());

        Optional<ProductDTO> result = productService.getProductById(9L);

        assertThat(result).isEmpty();
        verify(productRepository).findById(9L);
        verifyNoInteractions(productMapper);
    }

    // ------------------- findProductsByDescriptionContaining -------------------

    @Test
    @DisplayName("findProductsByDescriptionContaining delegates to repository and maps list")
    void findProductsByDescriptionContaining_delegatesAndMaps() {
        String searchText = "phone";
        List<Product> entityList = List.of(phoneEntity);
        List<ProductDTO> dtoList = List.of(phoneDto);

        when(productRepository.findByDescriptionContainingIgnoreCase(searchText))
                .thenReturn(entityList);
        when(productMapper.productsToProductDTOs(entityList)).thenReturn(dtoList);

        List<ProductDTO> result = productService.findProductsByDescriptionContaining(searchText);

        assertThat(result).containsExactly(phoneDto);
        verify(productRepository).findByDescriptionContainingIgnoreCase(searchText);
        verify(productMapper).productsToProductDTOs(entityList);
    }

    // ------------------- createProduct -------------------

    @Test
    @DisplayName("createProduct saves entity and maps to DTO")
    void createProduct_savesAndMaps() {
        when(productRepository.save(any(Product.class))).thenReturn(savedEntity);
        when(productMapper.productToProductDTO(savedEntity)).thenReturn(savedDto);

        ProductDTO result = productService.createProduct(createRequest);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getDescription()).isEqualTo("New");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();
        assertThat(capturedProduct.getDescription()).isEqualTo("New");

        verify(productMapper).productToProductDTO(savedEntity);
    }

    // ------------------- updateProduct -------------------

    @Test
    @DisplayName("updateProduct throws ResourceNotFoundException when id is missing")
    void updateProduct_throwsWhenMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");

        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(productMapper);
    }

    @Test
    @DisplayName("updateProduct updates fields and maps to DTO")
    void updateProduct_updatesAndMaps() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(phoneEntity));
        when(productRepository.save(any(Product.class))).thenReturn(updatedEntity);
        when(productMapper.productToProductDTO(updatedEntity)).thenReturn(updatedDto);

        ProductDTO result = productService.updateProduct(1L, updateRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Updated");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedArgument = productCaptor.getValue();
        assertThat(savedArgument.getId()).isEqualTo(1L);
        assertThat(savedArgument.getDescription()).isEqualTo("Updated");

        verify(productMapper).productToProductDTO(updatedEntity);
    }

    // ------------------- deleteProduct -------------------

    @Test
    @DisplayName("deleteProduct throws ResourceNotFoundException when id does not exist")
    void deleteProduct_throwsWhenIdMissing() {
        when(productRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");

        verify(productRepository).existsById(9L);
        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteProduct deletes when id exists")
    void deleteProduct_deletesWhenExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }
}
