# Setup Guide - eCommerce Order Management System

##  Quick Setup (5 Minutes)

### Prerequisites

Before you begin, install:
- **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop)
- **Git** - [Download here](https://git-scm.com/downloads)


---

## 📥 Step 1: Get the Code

```bash
# Clone the repository
git clone <your-repo-url>

# Navigate to project directory
cd ecommerce-order-management-system
```

---

## 🐳 Step 2: Start the System

```bash
# Option A: Use the helper script (recommended)
./start-system.sh

# Option B: Direct docker-compose command
docker-compose up --build -d
```

**That's it!** The script will:
1. ✅ Check Docker is installed and running
2. ✅ Build all 5 microservices
3. ✅ Start all infrastructure (PostgreSQL, MongoDB, RabbitMQ, Redis)
4. ✅ Deploy the complete system

**Time:** ~5 minutes on first run, ~30 seconds on subsequent runs.

---

## ⏳ Step 3: Wait for Services

```bash
# Wait 90 seconds for all services to become healthy
echo "Waiting for services to start..."
sleep 90

# Check status - all should show (healthy)
docker-compose ps
```

**Expected output:**
```
NAME                             STATUS
ecommerce-auth-server            Up (healthy)   ✅
ecommerce-api-gateway            Up (healthy)   ✅
ecommerce-order-service          Up (healthy)   ✅
ecommerce-analytics-service      Up (healthy)   ✅
ecommerce-notification-service   Up (healthy)   ✅
ecommerce-postgres               Up (healthy)   ✅
ecommerce-mongodb                Up (healthy)   ✅
ecommerce-rabbitmq               Up (healthy)   ✅
ecommerce-redis                  Up (healthy)   ✅
```

---

## ✅ Step 4: Test the System

```bash
# Run the automated test
./test-system.sh
```

This will:
1. ✅ Get a JWT token from Authorization Server
2. ✅ Create an order via API Gateway
3. ✅ Verify event processing (RabbitMQ)
4. ✅ Check analytics updates (CQRS)
5. ✅ Confirm all services are healthy

**Success output:**
```
✅ Token obtained
✅ Order created successfully
✅ Analytics data retrieved
✅ All services are healthy

🎉 System is working correctly!
```

---

## 🌐 Access Points

Once running, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| **API Gateway** | http://localhost:8090 | Bearer Token required |
| **Authorization Server** | http://localhost:9000 | OAuth2 endpoints |
| **RabbitMQ Management** | http://localhost:15672 | guest / guest |
| **Order Service** | http://localhost:8080 | Via Gateway |
| **Analytics Service** | http://localhost:8082 | Via Gateway |
| **Notification Service** | http://localhost:8081 | Via Gateway |

---

## 📝 Common Commands

```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f order-service

# Check service status
docker-compose ps

# Stop the system
docker-compose down

# Stop and remove all data
docker-compose down -v

# Restart a specific service
docker-compose restart order-service

# Rebuild a specific service
docker-compose build --no-cache order-service
docker-compose up -d order-service
```

---

## 🔧 Manual Testing (Optional)

### 1. Get an OAuth Token

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -u ecommerce-api-gateway:gateway-secret-2024 \
  -d "grant_type=client_credentials"
```

**Response:**
```json
{
  "access_token": "eyJhbGci...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### 2. Create an Order

```bash
# Replace YOUR_TOKEN with the access_token from above
curl -X POST http://localhost:8090/api/v1/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 123,
    "customerEmail": "john@example.com",
    "paymentMethod": "CREDIT_CARD",
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Springfield",
      "state": "IL",
      "zipCode": "62701",
      "country": "USA"
    },
    "orderLines": [{
      "productId": 1001,
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 999.99
    }]
  }'
```

### 3. Check Analytics

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8090/api/v1/analytics/summary
```

---


## 🧹 Clean Start

If you want to start completely fresh:

```bash
# Stop everything
docker-compose down -v

# Remove images (optional - forces rebuild)
docker-compose down --rmi all -v

# Remove all Docker resources (nuclear option)
docker system prune -a --volumes

# Start fresh
docker-compose up --build -d
sleep 90
./test-system.sh
```

---

## 📚 Next Steps

After successful setup:

1.**Explore the Code:**
   ```
   ecommerce-order-management-system/
   ├── authorization-server/    # OAuth2 server
   ├── api-gateway/             # API Gateway
   ├── order-service/           # Order management
   ├── analytics-service/       # Analytics (CQRS read)
   ├── notification-service/    # Email notifications
   └── common-lib/              # Shared models
   ```


2.**Monitor the System:**
    - RabbitMQ UI: http://localhost:15672 (guest/guest)
    - Service health: http://localhost:8080/actuator/health
    - Logs: `docker-compose logs -f`

---

## 💡 Tips

- **First time?** Use `./start-system.sh` - it's interactive and helpful
- **Testing changes?** Rebuild specific service: `docker-compose build order-service`
- **Debugging?** Use `docker-compose logs -f service-name`
- **Performance?** Close other applications, Docker needs ~2GB RAM
- **Clean setup?** Run `docker-compose down -v` before rebuilding

---

## ✅ Verification Checklist

After setup, verify:

- [ ] All 9 containers running (`docker-compose ps`)
- [ ] All services showing **(healthy)**
- [ ] Test script passes (`./test-system.sh`)
- [ ] Can get OAuth token
- [ ] Can create order via API Gateway
- [ ] Analytics service updated
- [ ] RabbitMQ UI accessible

---

## 🎉 You're All Set!

Your eCommerce Order Management System is now running with:

- ✅ 6 Microservices (Authorization, Gateway, Order, Analytics, Notification, Common)
- ✅ OAuth 2.0 Security
- ✅ CQRS Pattern
- ✅ Event-Driven Architecture
- ✅ Complete Infrastructure (PostgreSQL, MongoDB, RabbitMQ, Redis)
