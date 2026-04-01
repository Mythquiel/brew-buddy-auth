#!/bin/bash

# Deployment script for brew-buddy-auth

set -e

echo "🚀 Brew Buddy Auth Deployment Script"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if flyctl is installed
if ! command -v flyctl &> /dev/null; then
    echo -e "${RED}❌ flyctl is not installed${NC}"
    echo "Install it with: brew install flyctl"
    echo "Or visit: https://fly.io/docs/hands-on/install-flyctl/"
    exit 1
fi

# Parse command line arguments
COMMAND=${1:-help}

case $COMMAND in
    local)
        echo -e "${YELLOW}🐳 Building and running locally with Docker...${NC}"
        docker-compose up --build
        ;;

    build)
        echo -e "${YELLOW}🔨 Building Docker image...${NC}"
        docker build -t brew-buddy-auth .
        echo -e "${GREEN}✅ Build complete!${NC}"
        ;;

    test)
        echo -e "${YELLOW}🧪 Testing Docker image locally...${NC}"
        docker run --rm -p 8081:8081 \
          -e JWT_SECRET="${JWT_SECRET}" \
          -e DB_URL="${DB_URL}" \
          -e DB_USERNAME="${DB_USERNAME}" \
          -e DB_PASSWORD="${DB_PASSWORD}" \
          brew-buddy-auth
        ;;

    secrets)
        echo -e "${YELLOW}🔐 Setting Fly.io secrets...${NC}"
        echo ""

        # Check if .env file exists
        if [ ! -f .env ]; then
            echo -e "${RED}❌ .env file not found${NC}"
            echo "Create a .env file with your secrets first"
            exit 1
        fi

        # Load .env file
        source .env

        # Set secrets
        flyctl secrets set \
          DB_URL="${DB_URL}" \
          DB_USERNAME="${DB_USERNAME}" \
          DB_PASSWORD="${DB_PASSWORD}" \
          JWT_SECRET="${JWT_SECRET}" \
          ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
          CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS}"

        echo -e "${GREEN}✅ Secrets set successfully!${NC}"
        ;;

    deploy)
        echo -e "${YELLOW}🚀 Deploying to Fly.io...${NC}"
        flyctl deploy
        echo -e "${GREEN}✅ Deployment complete!${NC}"
        echo ""
        echo "Check status: flyctl status"
        echo "View logs: flyctl logs"
        echo "Open app: flyctl open"
        ;;

    logs)
        echo -e "${YELLOW}📋 Fetching logs...${NC}"
        flyctl logs
        ;;

    status)
        echo -e "${YELLOW}📊 Checking app status...${NC}"
        flyctl status
        ;;

    open)
        echo -e "${YELLOW}🌐 Opening app in browser...${NC}"
        flyctl open
        ;;

    help|*)
        echo "Usage: ./deploy.sh [command]"
        echo ""
        echo "Commands:"
        echo "  local    - Build and run with Docker Compose"
        echo "  build    - Build Docker image locally"
        echo "  test     - Test Docker image locally"
        echo "  secrets  - Set Fly.io secrets from .env file"
        echo "  deploy   - Deploy to Fly.io"
        echo "  logs     - View Fly.io logs"
        echo "  status   - Check Fly.io app status"
        echo "  open     - Open app in browser"
        echo "  help     - Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./deploy.sh local           # Run locally"
        echo "  ./deploy.sh secrets         # Set secrets"
        echo "  ./deploy.sh deploy          # Deploy to production"
        ;;
esac
