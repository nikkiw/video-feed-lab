# Kotlin Multiplatform Multi-Module Template (`nikkiw/kmp-modular-template`)

[![CI](https://github.com/nikkiw/kmp-modular-template/actions/workflows/ci.yml/badge.svg)](https://github.com/nikkiw/kmp-modular-template/actions/workflows/ci.yml)

A clean starter template for Kotlin Multiplatform (KMP) applications targeting Android and Desktop.

The project uses a decoupled multi-module architecture with independent feature
modules and centralized Gradle convention plugins in `build-logic`.

## Features

- **Multiplatform Support:** Shared business logic and Compose UI entry points for:
  - Android application (`androidApp`)
  - Desktop application with Compose for Desktop (`desktopApp`)
- **Architecture:** Clean, decoupled design using Feature modules (with API and Implementation separation).
- **Linter & Code Formatting:**
  - **Detekt:** For static analysis and identifying code smells.
  - **Spotless (with KtLint):** For automated code formatting and style checking.
- **Convention Plugins:** Centralized build configuration located in the `build-logic` composite build.
- **CI/CD Ready:** Pre-configured GitHub Actions workflow for style checking, linting, and running unit tests.
- **Interactive Bootstrapper:** A shell script to rename packages, files, and project properties automatically.
- **Roadmap:** Web/Wasm and iOS targets are tracked in [ROADMAP.md](ROADMAP.md) and kept out of the default build until stable.

---

## Getting Started

### 1. Instantiate the Repository
Click the **"Use this template"** button on the [kmp-modular-template](https://github.com/nikkiw/kmp-modular-template) GitHub repository to create a new repository from this project, then clone it to your local machine.

### 2. Run the Interactive Setup Script
To customize the template for your new app (renaming packages, namespaces, and project slug), run the root setup script:

```bash
chmod +x setup.sh
./setup.sh
```

The script will prompt you for:
1. **New Project Name** (e.g., `My App`)
2. **New Package Namespace** (e.g., `com.company.myapp`)

*Note: The script automatically renames package declarations, file directories, update references, resets git (optional), and deletes itself once finished.*

---

## Project Structure

```
├── androidApp/          # Android client application entry point
├── desktopApp/          # Desktop client application entry point (JVM)
├── shared/              # Core shared business logic & common UI
├── feature/             # Modular feature modules
│   └── home/
│       ├── api/         # Home feature interfaces and API
│       └── impl/        # Home feature implementation
├── build-logic/         # Custom convention plugins (composite build)
├── config/detekt/       # Detekt code quality rule configuration
└── ROADMAP.md           # Planned Web/Wasm and iOS targets
```

---

## Code Style & Analysis

This project maintains code quality using automated checks.

### Formatter (Spotless)
To check code formatting:
```bash
./gradlew spotlessCheck
```
To automatically apply formatting and fix KtLint issues:
```bash
./gradlew spotlessApply
```

### Static Analysis (Detekt)
To run Detekt static analysis:
```bash
./gradlew detekt
```

### Running Tests
To execute all JVM unit tests:
```bash
./gradlew test
```

### Full Verification
To run the same broad verification expected from the template:
```bash
./gradlew build
```

---

## CI/CD Workflow

A GitHub Actions workflow is located in `.github/workflows/ci.yml`. It runs automatically on every push or pull request to the `main` or `master` branches, performing the following checks:
1. **Spotless Check:** Formats verification.
2. **Detekt Static Analysis:** Code health and design check.
3. **Unit Tests:** Executes JVM test suites across all modules.
4. **Full Build:** Builds Android/Desktop modules and runs Gradle checks.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
