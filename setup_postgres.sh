#!/bin/bash

# Script to set up PostgreSQL for the LLM Comparison Backend

echo "Setting up PostgreSQL for LLM Comparison Backend..."

# Try to create user and database
sudo -u postgres psql -c "CREATE USER sahal WITH SUPERUSER PASSWORD 'sahal';" 2>/dev/null || echo "User might already exist"
sudo -u postgres psql -c "CREATE DATABASE llm_comparison OWNER sahal;" 2>/dev/null || echo "Database might already exist"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE llm_comparison TO sahal;" 2>/dev/null || echo "Privileges might already be granted"

echo "PostgreSQL setup completed!"
echo "You can now run the application with: mvn spring-boot:run"
