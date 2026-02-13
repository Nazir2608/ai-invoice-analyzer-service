#!/bin/bash

echo "Setting up permanent Git cleanup..."

############################################
# 1️⃣ Create Global Git Ignore
############################################

GLOBAL_IGNORE="$HOME/.gitignore_global"

echo "Creating global gitignore at $GLOBAL_IGNORE"

cat <<EOL > $GLOBAL_IGNORE
# IntelliJ
.idea/
*.iml

# Maven
target/

# macOS
.DS_Store

# Logs
*.log

# Build
*.class
*.jar

# Node (future safety)
node_modules/

# Temp
*.tmp
*.swp
EOL

echo " Global gitignore created."

############################################
# 2️⃣ Configure Git To Use It
############################################

git config --global core.excludesfile "$GLOBAL_IGNORE"

echo " Git configured to use global ignore."

############################################
# 3️⃣ Clean Current Repo Cache
############################################

echo "🧹 Cleaning tracked junk files..."

git rm -r --cached . > /dev/null 2>&1

############################################
# 4️⃣ Re-add Clean Files
############################################

git add .

############################################
# 5️⃣ Commit Cleanup
############################################

git commit -m "enforce permanent gitignore cleanup" 2>/dev/null

echo " Repository cleaned permanently!"
echo " Junk files will never be tracked again."