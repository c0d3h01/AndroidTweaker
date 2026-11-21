# AndroidTweaker — dev shortcuts. Run `make help`.
# Requires JDK 17 and Android SDK Platform 36. See README.md.

GRADLEW := ./gradlew
APK := app/release/androidtweaker.apk
ENGINE := app/src/main/assets/tweaker

.DEFAULT_GOAL := help

help: ## Show this list
	@grep -E '^[a-z-]+[^:]*:.* ## ' $(MAKEFILE_LIST) | sort | awk -F'## ' '{split($$1, a, ":"); printf "  %-12s %s\n", a[1], $$2}'

doctor: ## Check toolchain (JDK 17, Gradle 9.2, Android SDK)
	@test -n "$$JAVA_HOME" || (echo "JAVA_HOME unset — point it at JDK 17"; exit 1)
	@"$$JAVA_HOME/bin/java" -version 2>&1 | head -n 1
	@$(GRADLEW) --version 2>/dev/null | grep -E "^(Gradle|JVM:)" || echo "wrapper dist download needed (network)"
	@test -n "$$ANDROID_HOME" || (echo "ANDROID_HOME unset"; exit 1)
	@test -d "$$ANDROID_HOME" || (echo "ANDROID_HOME missing: $$ANDROID_HOME"; exit 1)
	@test -f "$$ANDROID_HOME/platforms/android-36/android.jar" || (echo "Install Android SDK Platform 36"; exit 1)
	@echo "toolchain OK"

test: ## Run the JVM unit tests
	$(GRADLEW) :app:testDebugUnitTest

debug: ## Build debug APK
	$(GRADLEW) :app:assembleDebug

release: ## Build and stage a release with your configured signing key
	$(GRADLEW) :app:stageReleaseApk
	@ls -lh $(APK)

verify: ## Verify release signature (needs release built)
	apksigner verify --print-certs $(APK) | head -n 2

install: release ## Install release on connected device (adb)
	adb install -r $(APK)

lint-shell: ## Syntax-check the bundled engine scripts
	@for f in $(ENGINE)/lib/*.sh $(ENGINE)/lib/tweaks/*.sh $(ENGINE)/profiles/*.sh; do \
	  sh -n "$$f" || exit 1; \
	done; echo "shell OK"

test-shell: lint-shell ## Test charging policy against simulated device files
	sh app/src/test/shell/charging_test.sh

check: test test-shell ## JVM tests, shell syntax, and charging policy

clean: ## Remove build outputs
	$(GRADLEW) clean

.PHONY: help doctor test debug release verify install lint-shell test-shell check clean
