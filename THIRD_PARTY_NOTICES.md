# Third-party notices

These notices cover third-party materials, not the licensing of KurViber's own code.
The project's primary license is maintained separately by the repository owner.

## Xposed API 82

- File: `app/libs/api-82.jar`.
- Coordinates: `de.robv.android.xposed:api:82`.
- Upstream: https://github.com/rovo89/XposedBridge
- License: Apache License 2.0; see [license text](licenses/Apache-2.0.txt).
- Attribution: Copyright 2013 rovo89, Tungstwenty.
- Upstream notices: [XposedBridge-NOTICE.txt](licenses/XposedBridge-NOTICE.txt),
  preserved from https://github.com/rovo89/XposedBridge/blob/art/NOTICE.txt.
  This is the upstream project's notice, including its own included-library notices;
  it does not assert that every upstream library is present in this API stub JAR.
- Local provenance: copied unchanged from the supplied
  https://github.com/smudd/xposed-template, `app/libs/api-82.jar`.
- SHA-256: `f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25`.

The JAR is used only for compilation (`compileOnly`) and is not packaged in the
module APK. Runtime implementations are supplied by the user's Xposed framework.
A byte-for-byte comparison against the official download has not been completed:
the official repository was unavailable during verification. The hash above identifies
the exact bundled file; it is not a claim of verified upstream signature authenticity.

## Gradle Wrapper

- Files: `gradlew`, `gradle/wrapper/gradle-wrapper.jar` and wrapper configuration.
- Upstream: https://github.com/gradle/gradle
- License: Apache License 2.0; see [license text](licenses/Apache-2.0.txt).
- The original `gradlew` copyright/license header is preserved:
  Copyright 2015 the original author or authors.

The wrapper is build tooling and is not included in the module APK.

## Supplied Xposed template

- Source: https://github.com/smudd/xposed-template
- The supplied template README declares the MIT License but its checkout does not
  contain the referenced LICENSE file. No author-specific MIT notice has been invented.
- KurViber began with the supplied template's Java/legacy-Xposed build setup. The
  template's third-party Xposed JAR and Gradle Wrapper retain their own notices above.

The template's license declaration is not a replacement for the separate licenses of
its bundled dependencies.
