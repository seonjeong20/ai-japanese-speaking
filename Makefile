.PHONY: db up down test lint

db:
	docker compose up -d --wait db

up:
	docker compose up -d --build --wait

down:
	docker compose down

test: db
	cd backend && ./gradlew test

lint:
	cd frontend && npm run lint && npm run build
