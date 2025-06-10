"""# Project Git Guidelines

aa

Welcome to the project! To keep our development process smooth, trackable, and efficient, please follow these simple Git guidelines.

## Table of Contents
1.  [Branch Naming Convention](#branch-naming-convention)
2.  [Commit Message Convention](#commit-message-convention)

## 1. Branch Naming Convention

Clear branch names help everyone understand the purpose of each branch.

**General Structure:**

`<branch-type>/<short-description>`

**Common Branch Types:**

* `feature/<feature-name>`: For developing a new feature.
    * Example: `feature/user-login`, `feature/display-product-list`
* `fix/<issue-description>` or `bugfix/<issue-description>`: For fixing a bug.
    * Example: `fix/login-form-error`, `bugfix/image-not-loading`
* `docs/<topic>`: For writing or updating documentation.
    * Example: `docs/update-readme`, `docs/setup-guide`
* `refactor/<area-refactored>`: For code refactoring without changing functionality.
    * Example: `refactor/database-schema`, `refactor/ui-component-structure`
* `chore/<task-description>`: For routine tasks not directly related to features or fixes (e.g., updating build scripts, `.gitignore`).
    * Example: `chore/update-dependencies`, `chore/configure-linter`

**Notes:**
* Use lowercase letters.
* Separate words with hyphens (`-`).
* Keep the description brief but clear.

## 2. Commit Message Convention

Good commit messages help us understand what changes were made and why. We'll use a simple format.

**Basic Structure:**

`<type>: <short-summary-of-changes>`

**Common `<type>`:**

* **`feat`**: A new feature is added.
    * Example: `feat: add user registration page`
* **`fix`**: A bug is fixed.
    * Example: `fix: correct calculation error in shopping cart`
* **`docs`**: Changes to documentation.
    * Example: `docs: update installation instructions`
* **`style`**: Code style changes (formatting, missing semi-colons, etc.; no functional code change).
    * Example: `style: format code according to project guidelines`
* **`refactor`**: Rewriting or restructuring code, but not adding features or fixing bugs.
    * Example: `refactor: simplify user authentication logic`
* **`test`**: Adding new tests or correcting existing tests.
    * Example: `test: add unit tests for login service`
* **`chore`**: Other changes that don't modify source code or test files (e.g., updating build tools, package manager configs).
    * Example: `chore: update npm dependencies`

**`<short-summary-of-changes>` (Subject):**

* Use the present tense and imperative mood (e.g., "add", "fix", "change" NOT "added", "fixed", "changed").
* Start with a lowercase letter (unless it's a proper noun).
* Don't end the subject line with a period.
* Keep it short and descriptive (ideally under 50 characters).

**Good Commit Message Examples:**


feat: implement password reset functionality

fix: prevent duplicate entries in user list

docs: add API endpoint descriptions


Thanks for following these guidelines. Happy coding!
