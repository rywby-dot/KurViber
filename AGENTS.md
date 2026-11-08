# KurViber: guide for coding agents

## Purpose and user requirements

KurViber is a Java Xposed module for the original Viber Android application. It removes
advertising rows without leaving reserved space, suppresses known telemetry paths,
disables selected ChatGPT entry points, hides unwanted UI, and customizes the home
screen. The public repository directory is KurViber. Internal identifiers still use
Viber NoSlop / io.github.vibernoslop; this is intentional existing compatibility state.

Keep user-facing module UI, code identifiers, comments and diagnostics in English.
Respond to the project owner in Russian. The short public README is intentionally in
Russian. Keep README minimal: target version and enablement instructions. Put detailed
engineering information here and in ANALYSIS.md, not into the README.

The owner uploads APKs to GitHub Releases manually. Do not publish, push, tag, commit,
change remotes, or upload artifacts unless requested. APKs, source distribution ZIPs,
private signing keys, device screenshots and original Viber APKs must not enter Git.
Preserve all module source and files necessary to reproduce the build.

## Known target and runtime

- Original Viber package: com.viber.voip.
- Primary analyzed APK: 29.3.1.0, versionCode 1290301, Google Play installation.
- Supplied installation consists of base.apk, arm64-v8a and xxhdpi splits.
- User's device: Nothing Phone (3), Nothing OS 5, Android 17.
- Root environment reported by owner: KernelSU-Next and SUSFS via LKM, NeoZygisk.
- Hook framework: Vector 2.2 (3080), modern framework API 102.
- This module uses Vector's legacy Xposed compatibility API 82; it is not a modern
  libxposed API 102 module. Do not accidentally mix both entry-point formats.
- Module minSdk 32, compileSdk/targetSdk 35, Java source/target 11.
- Current module version: 0.2.3, versionCode 5.
- Older APK 29.0.1 was used to check AI discovery only. Do not claim full support for
  that version or for arbitrary future Viber versions.

The owner confirmed that the 0.2.0 UI changes work on their phone. An earlier report
that everything failed was retracted: they had installed an old APK. Version 0.2.1
added diagnostic detail; it was not evidence of a confirmed 0.2.0 failure. Version
0.2.2 changed search to invoke the active FAB directly. Version 0.2.3 changed defaults.
Build/static checks do not establish live behavior of changes made after device tests.

## Repository layout

- app/src/main/java/io/github/vibernoslop/: implementation.
- app/src/main/AndroidManifest.xml: legacy module metadata, launcher activity, scope.
- app/src/main/assets/xposed_init: io.github.vibernoslop.XposedModule.
- app/src/main/res/values/arrays.xml: scope restricted to com.viber.voip.
- app/libs/api-82.jar: required compile-only Xposed API; keep in Git.
- gradle/wrapper/gradle-wrapper.jar and .properties, gradlew: keep in Git.
- app/build.gradle.kts, build.gradle.kts, settings.gradle.kts: Gradle build.
- scripts/build-local.py: verified offline SDK/JDK build and local signing workflow.
- scripts/check.py: APK-based discovery, resource and packaging verification.
- tests/: host-JVM discovery and UI policy assertions.
- ANALYSIS.md: evidence from reverse engineering the primary Viber APK.
- build/: generated classes, intermediate APKs, tests and development signing key.
- dist/: generated installable APKs and optional source archives; ignored by Git.

Original APKs/decompiler output may exist outside this repository in the local
workspace. They are optional analysis inputs, not dependencies to commit or redistribute.
Do not rely on machine-specific absolute paths when adding build tooling.

## Build

Verified path from repository root:

    python3 scripts/build-local.py
    python3 scripts/check.py /path/to/original/base.apk

Requirements: JDK 17+ with javac, java, jar, keytool; Python 3; Android SDK platform 35
and build-tools 36.0.0. SDK path defaults to ~/Android/Sdk and can be overridden with
ANDROID_SDK_ROOT. Build-tool directory can be overridden with VIBER_BUILD_TOOLS.
The check script currently uses build-tools 36.0.0 explicitly.

The offline script packages resources/assets with aapt, compiles Java against
android.jar and api-82.jar, runs D8, aligns the APK and signs it. It creates
build/noslop-debug.jks if missing. This is a development signing key. Keep the existing
local key to sign compatible updates; do not delete or commit it. Generating a new key
changes the update signing identity. Do not request users to uninstall Viber to solve
module-signing problems.

Alternative Gradle build: ./gradlew :app:assembleDebug. AGP 8.10.1 and Gradle 8.11.1;
dependency downloads may be required. The offline build is the established verified path.

Versions are duplicated: update app/build.gradle.kts, scripts/build-local.py,
scripts/check.py, the SettingsUi report label and XposedModule build diagnostic when
bumping a release. Increment versionCode for each installable update. Verify the
manifest inside the built APK rather than assuming the Gradle version was used by the
offline build. Existing output naming uses viber-noslop-VERSION.apk; changing it needs
coordinated script updates, not a blind global identifier rename.

## Architecture

### Entry point and configuration

XposedModule filters package load to com.viber.voip and installs hooks before
Application.attach completes. Hook groups run independently through safe(). Privacy
hooks apply in Viber processes; UI hooks/discovery are restricted to the main process.
A missing optional class must not crash Viber or disable unrelated hook groups.

Options stores preferences inside Viber's private viber_noslop SharedPreferences.
Boolean options are snapshotted at process start. SettingsUi edits saved choices, and
the user force-stops/reopens Viber to activate them. Do not make adapter counts change
live midway through layout by replacing the snapshot with preference reads on every hook.
String settings currently read saved values directly. Discovery caches use separate
preferences and APK path/length/mtime to invalidate results after Viber updates.

All switches default to true. DEFAULT_ACCENT is exactly #ffffff. DEFAULT_TITLE is
exactly KurViber✅, including case and emoji. Previously saved user choices take
precedence; do not silently overwrite them on upgrade. Scope and Android identifiers
must remain compatible unless the owner requests migration.

### Advertising and layout

AdAdapterHooks locates the common obfuscated ad adapter via the stable named
PreferencesWithAdsRecyclerAdapterWrapper superclass. isAdAvailable controls the
additional row, item count, IDs and position mapping. Placement policy is derived from
resource-entry names in integer fields, not hardcoded resource integers. This removes
the ad before creating its row. Chats and More have separate removal switches.

UiRules maps stable resource-entry names to options. UiHooks marks selected roots and
enforces GONE, zero height/margins/minimum height/padding, and measurement suppression.
A unique RecyclerView Rect(View) method provides zero decoration insets for collapsed
rows. Do not merely block ad loading: empty skeletons or gaps violate a primary requirement.
Leave ViewStub inflation semantics intact; collapse the inflated translation button.
Preserve navigation arrays/indices and Viber's normal adapter mappings.

### Home controls and settings

SettingsEntry inserts a Viber NoSlop section above settingsFragmentHolder while keeping
the holder ID and parent layout constraints. SettingsUi opens the module options dialog.
MainActivity is a setup-information launcher, not the authoritative preference editor.

HomeControls attaches to activity_home_toolbar and refreshes after toolbar layout,
because Viber creates menu items asynchronously and resets its styled title. It uses
the actual toolbar Menu to hide menu_camera and append a gear after search. The gear
opens the More tab by clicking its existing hidden navigation item. It is not a
hamburger and should not be changed back into one. Gear drawing follows the accent and
gradient controls. The toolbar long press remains an alternative module-settings entry.

When SEARCH_FAB is enabled, tapping search immediately calls performClick on the
currently VISIBLE, enabled FAB child with a click listener. No chooser, intermediate
menu or long-press requirement. Child getVisibility is used: isShown would always be
false when NoSlop hides the FAB container. Recognized children are fab_compose,
fab_open_keypad and fab_compose_video_call. Keep Viber's original click behavior instead
of rebuilding its destinations with guessed intents. Hiding FAB and transferring its
action are separate options, both enabled by default.

### Appearance

Appearance processes purple GradientDrawable backgrounds, GradientImageView drawable
masks and theme TypedArray color reads. Purple classification uses hue/saturation;
do not recolor every image, message bubble, avatar or attachment. FLAT removes gradients;
ACCENT uses the selected color. NO_BLUR separately bypasses blur rendering in
DynamicBlurLayout and BlurFrameLayout using the normal base drawing path, preserving
children. Gradients are not the same thing as blur.

The custom title must replace Viber's styled/image-span title after later Viber
updates, not merely set an initial title that gets overwritten. Avoid infinite layout
loops: only apply changed title/logo state. Appearance scan errors must be isolated
so they cannot prevent HomeControls or other UI hooks from installing.

### AI and obfuscation resistance

DexFinder is a small read-only DEX string-reference index with instruction/payload
width handling. ExperimentHooks locates the mapper using the diagnostic anchor
Too much time spent on mapping experiment= and validates a unique class/method shape.
Only core_ai_chatgptintegration, core_ai_chatgpttab and core_ai_chatgpttabv2 are assigned
null disabled results, verified nullable in the analyzed APK. Do not null arbitrary
experiment outputs: other feature consumers require non-null domain models.

The mapper was pvw in 29.3.1 and rls in 29.0.1. These are analysis examples, not
names to hardcode. UI resource-entry names, retained public API names, diagnostic
strings and validated signatures are preferred over obfuscated names and integer IDs.
Ambiguous fingerprints should skip that feature and log the reason, never select an
arbitrary match. Fingerprints still require validation when underlying semantics change.

### Privacy and known limits

PrivacyHooks covers selected Adjust, Mixpanel, Braze, Firebase, Google/Rakuten ads,
advertising ID and Viber CDR/data-SDK entry points. Data-SDK model Boolean fields
control directOutput/dataSdkOutput. Rakuten requests return asynchronous NO_BIDS through
its existing completion callback. Some suppressed ad-loading paths can still wait or
time out; avoid introducing initialization hangs or unbounded retry loops.

SdkDiscovery handles APIs obfuscated in the primary APK: Braze disabled-state getter,
Crashlytics collection arbiter, Google native ad request and related banner wrapper.
Examples in ANALYSIS.md explain the anchors and signature validation. Legacy named
class hooks remain optional fallbacks. ClassNotFoundException on those fallbacks does
not by itself mean the corresponding structural hook failed.

MetadataHooks overrides analytics/crash collection metadata in process. TrackerNetwork
blocks an explicit set of tracker domains through Java InetAddress methods. This does
not establish complete network isolation: native DNS, direct-IP requests, cached
connections, WebViews and unknown providers can bypass it. The owner observed Facebook
trackers through DuckDuckGo tracking protection; do not claim complete telemetry removal
without actual destination/network evidence. Do not casually block Viber messaging,
calling, authentication or attachment infrastructure to make tracker counters disappear.
No chats, tokens, contact names or message text belong in diagnostic output.

## Validation and troubleshooting

Run checks appropriate to the change. Build changes require a complete successful APK
build/signature check. Hook/discovery changes require checks against the original APK.
The check script verifies DEX anchors, UI policy assertions, all explicit target resource
names, entry-point assets and packaging. Those checks do not execute Android views or
prove hooks install, that screens have no gaps, or that telemetry is absent.

Runtime logs are tagged ViberNoSlop. HookLog deduplicates entries. SettingsUi can copy
hook diagnostics; RuntimeDiagnostics reports class/resource names and hierarchy for the
home toolbar without private text. Include accurate build identity and useful error
messages so stale Vector module loads can be distinguished from actual regressions.
If a tester reports no changes, verify the loaded build and saved options before adding
new speculative hooks. A previous apparent regression was simply an older installed APK.

Device checks for UI changes: cold launch, Chats/More ad rows before/after scrolling,
Translate, folders, camera, gear access with all lower navigation hidden, custom title
following tab changes, search invoking the currently appropriate FAB directly, gradient,
accent and blur options independently. Check restoration after disabling options and
restarting. For privacy changes also check messages, background notifications, voice/video
calls and attachments. Use relevant Vector/logcat traces if an action fails.

If the environment cannot connect to ADB or no device is attached, say so and report
static checks separately. Never describe untested device behavior as confirmed. To
recover Viber from a module crash, disable this module in Vector and restart Viber;
do not tell users to clear Viber data or reinstall the messenger.

## Repository hygiene

.gitignore excludes dist/build/caches, APKs and distribution archives, keys, private
analysis inputs and captures. It deliberately preserves api-82.jar and Gradle wrapper
JAR. Before preparing a public change use git status, git diff --check and
 git ls-files --cached --others --exclude-standard
(from this repository) to inspect what could be published. Inspect tracked files too:
.gitignore does not remove already tracked secrets/artifacts. Do not stage private
inputs or remove local keys to clean the repository. Source archives, if requested,
must omit build, dist, caches, private keys and original Viber data.
