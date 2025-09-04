# Store Application

An enterprise-grade Spring Boot application that manages customers and orders with comprehensive REST APIs, following industry best practices and global standards.

## 🚀 Version 2.0 - What's New

This application has been significantly enhanced with:
- **Enterprise Architecture**: Proper service layer, SOLID principles, dependency injection
- **REST API Best Practices**: Versioned APIs, proper HTTP status codes, Location headers
- **Enhanced Security**: Security headers, CORS configuration, input validation
- **Performance Optimization**: Database indexes, connection pooling, N+1 query prevention
- **Production Ready**: Environment-specific configurations, monitoring, comprehensive logging

## 📋 Features

- **Customer Management**: Create, read, update, delete customers with search functionality
- **Order Management**: Full CRUD operations for orders with customer associations
- **RESTful APIs**: Industry-standard REST endpoints with proper HTTP semantics
- **Database Integration**: PostgreSQL with Liquibase migrations and optimized queries
- **Validation**: Comprehensive input validation with business rule enforcement
- **Error Handling**: Structured error responses with specific error codes
- **Monitoring**: Health checks, metrics, and Prometheus integration
- **Security**: Security headers, CORS support, and validation layers
- **Documentation**: Comprehensive Javadoc and API documentation

# Assumptions
This README assumes you're using a posix environment. It's possible to run this on Windows as well:
* Instead of `./gradlew` use `gradlew.bat`
* The syntax for creating the Docker container is different. You could also install PostgreSQL on bare metal if you prefer


# Prerequisites

## Database (PostgreSQL)
This service requires a PostgreSQL 16.2 database server running on localhost:5432
- Username: `admin`
- Password: `admin` 
- Database: `store`

You can start the PostgreSQL instance like this:
```shell
docker run -d \
  --name postgres \
  --restart always \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=store \
  -v postgres:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:16.2 \
  postgres -c wal_level=logical
```

## Cache (Redis)
For optimal performance, the application uses Redis for caching frequently accessed data.
Redis runs on the standard port 6379 by default.

You can start a Redis instance like this:
```shell
docker run -d \
  --name redis \
  --restart always \
  -p 6379:6379 \
  redis:7-alpine \
  redis-server --appendonly yes
```

### Cache Configuration
- **Development**: 10-minute TTL, cache management endpoints enabled
- **Production**: 30-minute TTL, optimized connection pool
- **Environment Variables**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

# Running the application
You should be able to run the service using
```shell
./gradlew bootRun
```

The application uses Liquibase to migrate the schema. Some sample data is provided. You can create more data by reading the documentation in utils/README.md

# Data model
- A **customer** has an ID, a name, and 0 or more orders.
- An **order** has an ID, a description, is associated with a customer, and contains 1 or more products.
- A **product** has an ID, a description, and appears in 0 or more orders.

The application models a many-to-many relationship between orders and products through a junction table.

# 🔌 API Endpoints

## Version 2.0 REST API

The application provides RESTful APIs following industry standards:

### Customer Endpoints
- `GET /api/v1/customers` - Get all customers (with optional name search)
- `GET /api/v1/customers/paged` - Get customers with pagination
- `GET /api/v1/customers/{id}` - Get customer by ID
- `POST /api/v1/customers` - Create new customer (returns Location header)
- `PUT /api/v1/customers/{id}` - Update customer
- `DELETE /api/v1/customers/{id}` - Delete customer

### Order Endpoints
- `GET /api/v1/orders` - Get all orders (with products)
- `GET /api/v1/orders/paged` - Get orders with pagination
- `GET /api/v1/orders/{id}` - Get order by ID (with products)
- `GET /api/v1/orders/customers/{customerId}` - Get orders for specific customer
- `POST /api/v1/orders` - Create new order (returns Location header)
- `PUT /api/v1/orders/{id}` - Update order
- `DELETE /api/v1/orders/{id}` - Delete order

### Product Endpoints
- `GET /api/v1/products` - Get all products (with order IDs)
- `GET /api/v1/products?description=search` - Search products by description
- `GET /api/v1/products/paged` - Get products with pagination
- `GET /api/v1/products/{id}` - Get product by ID (with order IDs)
- `POST /api/v1/products` - Create new product (returns Location header)
- `PUT /api/v1/products/{id}` - Update product
- `DELETE /api/v1/products/{id}` - Delete product

### API Features
- **Versioning**: All endpoints prefixed with `/api/v1/` for backward compatibility
- **Pagination**: Configurable page size (max 100, default 20)
- **Search**: Name-based customer search with case-insensitive matching
- **Validation**: Comprehensive input validation with detailed error messages
- **Error Handling**: Structured error responses with specific error codes

## Data Transfer Objects (DTOs)

The API uses optimized DTOs to prevent circular references:
- **CustomerDTO**: Customer with simplified order information (CustomerOrderDTO)
- **OrderDTO**: Order with simplified customer information (OrderCustomerDTO) and products (OrderProductDTO)
- **ProductDTO**: Product with list of order IDs that contain this product
- **Request DTOs**: CustomerRequest, OrderRequest, and ProductRequest for create/update operations

## Error Response Format

```json
{
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Customer not found with id: 123",
  "timestamp": "2025-01-04T10:30:00",
  "path": "/api/v1/customers/123"
}
```

## Example API Usage

### Create Customer
```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe"}'
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"description": "Order description", "customerId": 1}'
```

### Create Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"description": "Laptop Computer"}'
```

### Get Customers with Pagination
```bash
curl "http://localhost:8080/api/v1/customers/paged?page=0&size=10&sort=name,asc"
```

# 🛠️ Configuration & Environment

## Environment Profiles

The application supports multiple environment profiles:

### Development (`application-dev.yaml`)
- Debug logging enabled
- SQL query logging with formatting
- All actuator endpoints exposed
- Detailed health check information

### Production (`application-prod.yaml`)
- Optimized connection pooling (20 max connections)
- Environment variable support for credentials
- Structured logging with file output
- Prometheus metrics enabled
- Restricted actuator endpoints

## Environment Variables

For production deployment, use these environment variables:
- `DB_USERNAME`: Database username (default: admin)
- `DB_PASSWORD`: Database password (default: admin)
- `SPRING_PROFILES_ACTIVE`: Active profile (dev/prod)

## Running with Profiles

```bash
# Development
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
./gradlew bootRun --args='--spring.profiles.active=prod'
```

# 📊 Monitoring & Health

## Actuator Endpoints

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus` (production only)

## Performance Features

- **Database Indexes**: Optimized queries with proper indexing
- **Connection Pooling**: Hikari connection pool with production tuning
- **N+1 Query Prevention**: EntityGraph and JOIN FETCH optimizations
- **Pagination Limits**: Maximum 100 items per page to prevent performance issues
- **Redis Caching**: Multi-layer caching strategy with intelligent cache eviction
  - Individual entity caching (customers, orders)
  - Search result caching
  - Relationship query caching
  - Configurable TTL per cache type

# 🏗️ Architecture

## Layer Structure
```
┌─────────────────┐
│   Controllers   │  ← REST endpoints, validation, HTTP handling
├─────────────────┤
│    Services     │  ← Business logic, transactions
├─────────────────┤
│  Repositories   │  ← Data access, query optimization
├─────────────────┤
│    Entities     │  ← JPA entities with indexes
└─────────────────┘
```

## Key Design Patterns
- **Service Layer Pattern**: Business logic abstraction
- **DTO Pattern**: Data transfer optimization
- **Repository Pattern**: Data access abstraction
- **Exception Handling**: Global exception management

# 📝 Documentation

- **Comprehensive Javadoc**: All public APIs documented
- **API Examples**: Included in this README
- **Change Log**: See `CHANGELOG.md` for detailed changes
- **Improvements Guide**: See `IMPROVEMENTS.md` for technical details

# 🎯 Completed Enhancements (Version 2.0)

## ✅ Originally Requested Tasks
1. **✅ Order by ID endpoint**: `GET /api/v1/orders/{id}`
2. **✅ Customer search**: `GET /api/v1/customers?name=searchTerm`
3. **✅ Performance optimizations**: Database indexes, N+1 query prevention, connection pooling
4. **✅ Products endpoint**: Complete `/api/v1/products` API with order relationships

## ✅ Additional Enterprise Improvements
- **Architecture**: Service layer implementation, SOLID principles
- **Security**: Security headers, CORS, input validation
- **API Design**: REST conventions, versioning, proper HTTP status codes
- **Error Handling**: Structured responses with error codes
- **Configuration**: Environment profiles, externalized configuration
- **Caching**: Redis-based multi-layer caching with intelligent eviction
- **Documentation**: Comprehensive Javadoc and user guides
- **Monitoring**: Health checks, metrics, logging improvements

# 🚀 Future Roadmap

## Suggested Enhancements
- **Authentication**: JWT or OAuth2 implementation
- **Advanced Caching**: Cache clustering and distributed cache invalidation
- **Event-Driven Architecture**: Async processing with events
- **Container Deployment**: Docker and Kubernetes support
- **CI/CD Pipeline**: Automated testing and deployment

## 🗄️ Cache Management (Development)

Cache management endpoints are available in development mode:

```bash
# Evict specific customer cache
DELETE /api/v1/admin/cache/customers/{customerId}

# Evict customer's orders cache  
DELETE /api/v1/admin/cache/customers/{customerId}/orders

# Clear customer search cache
DELETE /api/v1/admin/cache/searches/customers

# Warmup caches
POST /api/v1/admin/cache/warmup

# Get cache statistics
GET /api/v1/admin/cache/stats/{cacheName}
```
