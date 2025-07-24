# SEIMA SERVER - Smart Financial Management

[![Build & Unit Tests](https://github.com/seima-lab/seima-server/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/seima-lab/seima-server/actions/workflows/pr-validation.yml)
[![Deploy to Production](https://github.com/seima-lab/seima-server/actions/workflows/deploy-production.yml/badge.svg)](https://github.com/seima-lab/seima-server/actions/workflows/deploy-production.yml)

**SEIMA (Smart Financial Management)** is an intelligent financial management application for individuals and groups, built on the Spring Boot framework. This project provides a powerful API suite for tracking expenses, managing budgets, and collaborating on finances with family and friends.

## Table of Contents

- [Core Features](#core-features)
- [Technology Stack](#technology-stack)
- [System Requirements](#system-requirements)
- [Installation Guide](#installation-guide)
- [Environment Configuration](#environment-configuration)
- [API Overview](#api-overview)
- [CI/CD](#cicd)
- [Git Conventions](#git-conventions)

## Core Features

The project offers a comprehensive range of financial management features:

* **👤 User Management & Authentication:**
    * Register and log in with email/password and Google.
    * Authenticate using OTP via email.
    * Manage personal profiles and update avatars.
    * Deactivate accounts.
    * Secure authentication mechanism using JSON Web Tokens (JWT).

* **👥 Group Management:**
    * Create, update, and archive groups.
    * Manage members with different roles (Owner, Admin, Member).
    * Invite members via email and invitation links (using Branch.io).
    * Handle complex workflows for owners leaving a group (transferring ownership or deleting the group).

* **💰 Transaction Management:**
    * Record income and expense transactions.
    * Categorize transactions.
    * Automatically scan receipts using OCR technology (integrated with Azure Form Recognizer and Gemini).
    * View personal and group transaction history.

* **🏦 Wallet Management:**
    * Create and manage multiple financial wallets.
    * Support for various wallet types (e.g., cash, bank accounts).

* **🎯 Budget Management:**
    * Set up budgets on a monthly, quarterly, annual, or custom basis.
    * Allocate budget limits for specific spending categories.
    * Receive alerts when approaching budget limits.

* **🔔 Notification System:**
    * Send push notifications via Firebase Cloud Messaging (FCM).
    * Notify users of group activities (join requests, new members, etc.).

* **☁️ Cloud Service Integration:**
    * Store images (avatars, receipts) on Cloudinary.
    * Use Redis for caching and token management.

## Technology Stack

* **Backend:** Spring Boot 3, Java 21
* **Database:** MySQL, Spring Data JPA
* **Security:** Spring Security, JWT
* **Caching:** Redis
* **File Storage:** Cloudinary
* **Push Notifications:** Firebase Cloud Messaging
* **Deep Linking:** Branch.io
* **Dependency Management:** Maven
* **API Documentation:** SpringDoc OpenAPI (Swagger)
* **Email Handling:** Spring Boot Starter Mail, Thymeleaf

## System Requirements

* Java (JDK) 21
* Maven 3.9+
* MySQL Server 8.0+
* Redis

## Installation Guide

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/seima-lab/seima-server.git](https://github.com/seima-lab/seima-server.git)
    cd seima-server
    ```

2.  **Configure `application-dev.yaml`:**
    Copy `application-dev.yaml` and configure the necessary settings, such as database credentials, Redis connection, and API keys for third-party services (see Environment Configuration for details).

3.  **Build the project:**
    ```bash
    ./mvnw clean package
    ```

4.  **Run the application:**
    ```bash
    java -jar target/seima-server-0.0.1-SNAPSHOT.jar
    ```
    The application will run at `http://localhost:8081` (based on the default configuration in `application-dev.yaml`).

## Environment Configuration

Sensitive information is managed through environment variables. Please set the following variables before running the application:

* `DB_URL`: Connection URL for the MySQL database.
* `DB_USERNAME`, `DB_PASSWORD`: Database login credentials.
* `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`: Redis connection details.
* `YOUR_GOOGLE_CLIENT_ID`, `YOUR_GOOGLE_CLIENT_SECRET`: Credentials for Google Login.
* `MAIL_USERNAME`, `MAIL_PASSWORD`: Credentials for the email server (e.g., Gmail).
* `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`: Cloudinary credentials.
* `AZURE_FORM_END_POINT`, `AZURE_FORM_API_KEY`: Azure Form Recognizer credentials.
* `GEMINI_API_KEY`: API Key for Gemini.
* `BRANCH_IO_API_KEY`, `BRANCH_IO_SECRET`, `BRANCH_IO_DOMAIN`: Branch.io credentials.

## API Overview

Once the application is running, you can access the Swagger API documentation at:
`http://localhost:8081/swagger-ui.html`

Key endpoints include:
* `/api/v1/auth`: Authentication, registration, login, forgot password.
* `/api/v1/users`: User profile management.
* `/api/v1/groups`: Group management.
* `/api/v1/group-members`: Group member management.
* `/api/v1/transactions`: Transaction management.
* `/api/v1/wallets`: Wallet management.
* `/api/v1/budgets`: Budget management.
* `/api/v1/categories`: Category management.
* `/api/v1/notifications`: Notification management.

For detailed documentation on the group member management module, please refer to `docs/GROUP_MEMBER_API_DOCUMENTATION.md`.

## CI/CD

The project comes with pre-configured CI/CD pipelines using GitHub Actions:

* **PR Validation (`pr-validation.yml`):** Automatically builds the project and runs unit tests for every pull request targeting the `main` or `dev` branch.
* **Deploy to Production (`deploy-production.yml`):** Automatically deploys the application to an Azure Web App on every push to the `main` branch.

## Git Conventions

To ensure a consistent and efficient development workflow, please adhere to the Git conventions outlined in the original project `README.md`.

### Branch Naming Convention

* **feature/\<feature-name\>**: For developing new features.
* **fix/\<issue-description\>**: For bug fixes.
* **docs/\<topic\>**: For writing or updating documentation.
* **refactor/\<scope\>**: For code refactoring.
* **chore/\<task-name\>**: For tasks not directly related to the codebase.

### Commit Message Convention

Use the format: `<type>: <subject>`
* **`feat`**: A new feature.
* **`fix`**: A bug fix.
* **`docs`**: Documentation only changes.
* **`style`**: Changes that do not affect the meaning of the code.
* **`refactor`**: A code change that neither fixes a bug nor adds a feature.
* **`test`**: Adding missing tests or correcting existing tests.
* **`chore`**: Other changes that don't modify `src` or `test` files.

---

Happy coding, and enjoy working on the SEIMA project!
