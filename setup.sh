#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

# Interactive Setup Script for Kotlin Multiplatform Template
# This script renames project packages, modules, and titles.

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0;0m' # No Color

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}   Kotlin Multiplatform Project Template Bootstrapper   ${NC}"
echo -e "${BLUE}====================================================${NC}"

# 1. Get Project Name
default_project_name="My KMP App"
read -r -p "Enter new project name [default: $default_project_name]: " INPUT_PROJECT_NAME
PROJECT_NAME="${INPUT_PROJECT_NAME:-$default_project_name}"

# Slugify project name for rootProject.name in settings.gradle.kts
# e.g., "My KMP App" -> "my-kmp-app"
PROJECT_SLUG=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-zA-Z0-9]/-/g' | sed 's/-\{1,\}/-/g' | sed 's/^-//' | sed 's/-$//')

# 2. Get Package Name
default_package="com.mycompany.app"
while true; do
    read -r -p "Enter new package name [default: $default_package]: " INPUT_PACKAGE
    NEW_PACKAGE="${INPUT_PACKAGE:-$default_package}"
    
    # Simple regex for package validation (e.g. com.example.app)
    if [[ "$NEW_PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
        break
    else
        echo -e "${RED}Invalid package name. Must be lowercase letters/numbers separated by dots (e.g. com.company.app).${NC}"
    fi
done

NEW_PACKAGE_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')

echo -e "\n${BLUE}Configuring template with:${NC}"
echo -e "  - Project Name:   ${GREEN}$PROJECT_NAME${NC}"
echo -e "  - Project Slug:   ${GREEN}$PROJECT_SLUG${NC}"
echo -e "  - Package Name:   ${GREEN}$NEW_PACKAGE${NC}"
echo -e "  - Package Path:   ${GREEN}$NEW_PACKAGE_PATH${NC}\n"

read -r -p "Proceed with customization? (y/N): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[yY]([eE][sS])?$ ]]; then
    echo -e "${RED}Aborted.${NC}"
    exit 1
fi

echo -e "\n${BLUE}[1/5] Cleaning duplicate gradle properties files...${NC}"
# Delete the redundant manually-defined gradle-plugins directory (Gradle generates these at compile time)
if [ -d "build-logic/src/main/resources/META-INF" ]; then
    rm -rf "build-logic/src/main/resources/META-INF"
fi

echo -e "${BLUE}[2/5] Replacing text references in files...${NC}"

# Find and replace com.example references
# Using perl because it is cross-platform (macOS and Linux) and handles replacements consistently.
find_files() {
    find . -type f \( \
        -name "*.kt" \
        -o -name "*.kts" \
        -o -name "*.xml" \
        -o -name "*.properties" \
        -o -name "*.yml" \
        -o -name "README.md" \
    \) \
    -not -path "*/.git/*" \
    -not -path "*/build/*" \
    -not -path "*/.gradle/*"
}

# Replace package names
find_files | while read -r file; do
    perl -pi -e "s/com\.example/$NEW_PACKAGE/g" "$file"
done

# Replace root project name slug in settings.gradle.kts
if [ -f "settings.gradle.kts" ]; then
    perl -pi -e "s/kmp-modular-template/$PROJECT_SLUG/g" settings.gradle.kts
fi

echo -e "${BLUE}[3/5] Moving directory structures...${NC}"

# Find all directories that contain our template namespace segment
# In our project structure, directories to move end with /kotlin/com/example
find . -type d -path "*/src/*/kotlin/com/example" -not -path "*/build/*" | while read -r dir; do
    # e.g., dir = "./shared/src/commonMain/kotlin/com/example"
    base_path="${dir%/com/example}" # e.g., "./shared/src/commonMain/kotlin"
    target_dir="$base_path/$NEW_PACKAGE_PATH"
    
    echo "Moving contents of $dir to $target_dir"
    mkdir -p "$target_dir"
    
    # Copy all files/folders from com/example to target package dir
    cp -R "$dir/"* "$target_dir/"
    
    # Clean up old com/example directory
    rm -rf "$dir"
    
    # Clean up empty parent directories up to the kotlin folder
    # e.g., remove "com" if it is empty now
    parent_com="${dir%/example}"
    if [ -d "$parent_com" ] && [ -z "$(ls -A "$parent_com")" ]; then
        rm -rf "$parent_com"
    fi
done

echo -e "${BLUE}[4/5] Cleaning up build, IDE, and local files...${NC}"
rm -rf .gradle .kotlin .idea build build-logic/.gradle build-logic/.kotlin build-logic/build
rm -rf androidApp/.idea androidApp/build desktopApp/build shared/build feature/*/build
rm -rf kotlin-js-store java_pid*.hprof local.properties

echo -e "${BLUE}[5/5] Resetting git repository...${NC}"
read -r -p "Re-initialize Git repository? (Recommended for starting a new project) (y/N): " RESET_GIT
if [[ "$RESET_GIT" =~ ^[yY]([eE][sS])?$ ]]; then
    rm -rf .git
    git init
    git add .
    echo -e "${GREEN}Git repository re-initialized!${NC}"
fi

# Self-destruct setup script
rm -- "$0"

echo -e "\n${GREEN}✔ Setup complete! Your project '$PROJECT_NAME' is ready.${NC}"
echo -e "You can run ${BLUE}./gradlew build${NC} or ${BLUE}./gradlew spotlessApply${NC} to start."
