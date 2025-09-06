# Store API - Exception Handling Testing Guide

This document provides comprehensive testing instructions for the Store API's exception handling using Postman.

## Base URL
```
http://localhost:8081
```

## Test Scenarios

### 1. Resource Not Found (404) - ResourceNotFoundException

#### Test Case: Get Non-existent Customer
- **Method**: GET
- **URL**: `http://localhost:8081/api/v1/customers/99999`
- **Expected Response**:
  ```json
  {
    "status": 404,
    "error": "RESOURCE_NOT_FOUND",
    "message": "Customer not found with id: 99999",
    "timestamp": "2025-09-06T19:22:53.398478103",
    "path": "uri=/api/v1/customers/99999"
  }
  ```

### 2. Validation Errors (400) - MethodArgumentNotValidException

#### Test Case: Create Customer with Empty Name
- **Method**: POST
- **URL**: `http://localhost:8081/api/v1/customers`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (JSON):
  ```json
  {
    "name": ""
  }
  ```
- **Expected Response**:
  ```json
  {
    "status": 400,
    "message": "Request validation failed",
    "timestamp": "2025-09-06T19:22:59.883125024",
    "errors": {
      "name": "Customer name is required"
    }
  }
  ```

#### Test Case: Create Customer with Missing Name Field
- **Method**: POST
- **URL**: `http://localhost:8081/api/v1/customers`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (JSON):
  ```json
  {}
  ```
- **Expected Response**:
  ```json
  {
    "status": 400,
    "message": "Request validation failed",
    "timestamp": "2025-09-06T19:23:05.651733293",
    "errors": {
      "name": "Customer name is required"
    }
  }
  ```

### 3. Malformed JSON (400) - HttpMessageNotReadableException

#### Test Case: Invalid JSON Syntax
- **Method**: POST
- **URL**: `http://localhost:8081/api/v1/customers`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (Raw Text):
  ```
  {"name": invalid}
  ```
- **Expected Response**:
  ```json
  {
    "status": 400,
    "error": "MALFORMED_JSON",
    "message": "Request body is missing or malformed JSON",
    "timestamp": "2025-09-06T19:23:10.670053666",
    "path": "uri=/api/v1/customers"
  }
  ```

### 4. Success Cases (200/201)

#### Test Case: Get Existing Customer
- **Method**: GET
- **URL**: `http://localhost:8081/api/v1/customers/1`
- **Expected Response**:
  ```json
  {
    "id": 1,
    "name": "Muriel Donnelly",
    "orders": []
  }
  ```

#### Test Case: Get All Customers
- **Method**: GET
- **URL**: `http://localhost:8081/api/v1/customers`
- **Expected Response**: Array of customer objects with 200 status

#### Test Case: Create Valid Customer
- **Method**: POST
- **URL**: `http://localhost:8081/api/v1/customers`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (JSON):
  ```json
  {
    "name": "Test Customer via Postman"
  }
  ```
- **Expected Response**:
  ```json
  {
    "id": 108,
    "name": "Test Customer via Postman",
    "orders": []
  }
  ```

#### Test Case: Search Customers by Name
- **Method**: GET
- **URL**: `http://localhost:8081/api/v1/customers?name=Test`
- **Expected Response**: Array of customers with names containing "Test"

#### Test Case: Get Customers with Pagination
- **Method**: GET
- **URL**: `http://localhost:8081/api/v1/customers/paged?page=0&size=10`
- **Expected Response**: Paginated customer data with metadata

## Additional Test Scenarios

### 5. Update Customer

#### Test Case: Update Existing Customer
- **Method**: PUT
- **URL**: `http://localhost:8081/api/v1/customers/1`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (JSON):
  ```json
  {
    "name": "Updated Customer Name"
  }
  ```

#### Test Case: Update Non-existent Customer
- **Method**: PUT
- **URL**: `http://localhost:8081/api/v1/customers/99999`
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body** (JSON):
  ```json
  {
    "name": "Updated Customer Name"
  }
  ```
- **Expected Response**: 404 ResourceNotFoundException

### 6. Delete Customer

#### Test Case: Delete Existing Customer
- **Method**: DELETE
- **URL**: `http://localhost:8081/api/v1/customers/{id}`
- **Expected Response**: 204 No Content

#### Test Case: Delete Non-existent Customer
- **Method**: DELETE
- **URL**: `http://localhost:8081/api/v1/customers/99999`
- **Expected Response**: 404 ResourceNotFoundException

## Testing Instructions

1. **Import the Collection**: Import the provided Postman collection file
2. **Set Environment**: Create a Postman environment with:
   - `base_url`: `http://localhost:8081`
3. **Run Tests**: Execute requests in the following order:
   - Success cases first to verify API is working
   - Error cases to verify exception handling
4. **Verify Responses**: Check both status codes and response body structure
5. **Check Logs**: Monitor application logs for proper error logging

## Expected Behavior

- **Specific Exceptions**: Should be caught by their dedicated handlers
- **Consistent Format**: All error responses follow the same structure
- **Proper Status Codes**: HTTP status codes match the error types
- **Field Validation**: Validation errors include specific field information
- **Logging**: All exceptions should be properly logged in the application

## Notes

- The API uses proper HTTP status codes for different error types
- All timestamps are in ISO 8601 format
- Validation errors include a map of field-specific error messages
- The exception handling is scoped to the controller package only
- No Swagger/OpenAPI documentation is available (intentionally removed)