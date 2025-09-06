package com.securitease.store.service.impl;

import com.securitease.store.dto.ProductDTO;
import com.securitease.store.dto.ProductRequest;
import com.securitease.store.entity.Product;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.mapper.ProductMapper;
import com.securitease.store.repository.ProductRepository;
import com.securitease.store.service.CacheService;
import com.securitease.store.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the ProductService interface.
 *
 * <p>This service implementation provides concrete business logic for product management operations. It handles the
 * relationship between products and orders, ensuring data integrity and proper validation. Uses JPA repositories for
 * data access and MapStruct mappers for entity-to-DTO conversion.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see ProductService
 * @see ProductRepository
 * @see ProductMapper
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CacheService cacheService;

    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_SEARCH_CACHE = "product-search";

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.debug("Fetching all products");

        List<Product> products = productRepository.findAll();
        return productMapper.productsToProductDTOs(products);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProducts(Pageable pageable) {
        log.debug("Fetching products with pagination: {}", pageable);

        Page<Product> products = productRepository.findAll(pageable);
        return products.map(productMapper::productToProductDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_CACHE, key = "#id")
    public Optional<ProductDTO> getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);

        return productRepository.findById(id).map(productMapper::productToProductDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCT_SEARCH_CACHE, key = "#description.toLowerCase()")
    public List<ProductDTO> findProductsByDescriptionContaining(String description) {
        log.debug("Searching products with description containing: {}", description);

        List<Product> products = productRepository.findByDescriptionContainingIgnoreCase(description);
        return productMapper.productsToProductDTOs(products);
    }

    @Override
    @CachePut(value = PRODUCTS_CACHE, key = "#result.id")
    @CacheEvict(value = PRODUCT_SEARCH_CACHE, allEntries = true)
    public ProductDTO createProduct(ProductRequest request) {
        log.info("Creating new product with description: {}", request.getDescription());

        Product product = new Product();
        product.setDescription(request.getDescription());

        Product savedProduct = productRepository.save(product);
        log.info("Successfully created product with id: {}", savedProduct.getId());

        return productMapper.productToProductDTO(savedProduct);
    }

    @Override
    @CachePut(value = PRODUCTS_CACHE, key = "#id")
    @CacheEvict(value = PRODUCT_SEARCH_CACHE, allEntries = true)
    public ProductDTO updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product =
                productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setDescription(request.getDescription());
        Product updatedProduct = productRepository.save(product);

        log.info("Successfully updated product with id: {}", id);
        return productMapper.productToProductDTO(updatedProduct);
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(value = PRODUCTS_CACHE, key = "#id"),
                @CacheEvict(value = PRODUCT_SEARCH_CACHE, allEntries = true)
            })
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }

        productRepository.deleteById(id);
        log.info("Successfully deleted product with id: {}", id);
    }
}
