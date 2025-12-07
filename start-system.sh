#!/bin/bash

# eCommerce Order Management System - Docker Compose Startup Script
# This script helps you start the system with appropriate checks

set -e

echo "=========================================="
echo "eCommerce Order Management System"
echo "Docker Compose Deployment"
echo "=========================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    echo "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    echo "Please install Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✅ Docker and Docker Compose are installed${NC}"
echo ""

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo -e "${RED}❌ Docker daemon is not running${NC}"
    echo "Please start Docker Desktop or Docker service"
    exit 1
fi

echo -e "${GREEN}✅ Docker daemon is running${NC}"
echo ""

# Ask user for deployment mode
echo "Select deployment mode:"
echo "1) Fresh start (build images and start)"
echo "2) Quick start (use existing images)"
echo "3) Clean restart (remove volumes and rebuild)"
echo ""
read -p "Enter choice [1-3]: " choice

case $choice in
    1)
        echo ""
        echo -e "${YELLOW}Building and starting services...${NC}"
        echo "This may take 3-5 minutes on first run"
        docker-compose up --build -d
        ;;
    2)
        echo ""
        echo -e "${YELLOW}Starting services...${NC}"
        docker-compose up -d
        ;;
    3)
        echo ""
        echo -e "${YELLOW}Cleaning up and rebuilding...${NC}"
        docker-compose down -v
        docker-compose up --build -d
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
sleep 10

# Check service health
echo ""
echo "Service Status:"
docker-compose ps

echo ""
echo "=========================================="
echo -e "${GREEN}✅ System is starting up!${NC}"
echo "=========================================="
echo ""
echo "🌐 Access Points:"
echo "   - API Gateway:         http://localhost:8090"
echo "   - Authorization Server: http://localhost:9000"
echo "   - Order Service:        http://localhost:8080"
echo "   - Analytics Service:    http://localhost:8082"
echo "   - Notification Service: http://localhost:8081"
echo "   - RabbitMQ Management:  http://localhost:15672 (guest/guest)"
echo ""
echo "📊 Quick Test:"
echo "   Run: ./test-system.sh"
echo "   Or manually get a token and create an order (see DOCKER_COMPOSE_GUIDE.md)"
echo ""
echo "📝 View Logs:"
echo "   All services:     docker-compose logs -f"
echo "   Specific service: docker-compose logs -f order-service"
echo ""
echo "🛑 Stop System:"
echo "   docker-compose down"
echo ""
echo "📖 Full documentation: DOCKER_COMPOSE_GUIDE.md"
echo ""