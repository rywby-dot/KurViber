# KurViber: guide for coding agents

## Purpose and user requirements

KurViber is a Java Xposed module for the original Viber Android application. It removes
advertising rows without leaving reserved space, suppresses known telemetry paths,
disables selected ChatGPT entry points, hides unwanted UI, and customizes the home
screen. The public repository directory is KurViber. Public app/module/settings branding is KurViber. Internal package io.github.vibernoslop
and preference keys remain unchanged for update compatibility.

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
- Current module version: 0.5.3, versionCode 16.
- Older APK 29.0.1 was used to check AI discovery only. Do not claim full support for
  that version or for arbitrary future Viber versions.

The owner confirmed that the 0.2.0 UI changes work on their phone. An earlier report
that everything failed was retracted: they had installed an old APK. Version 0.2.1
added diagnostic detail; it was not evidence of a confirmed 0.2.0 failure. Version
0.2.2 changed search to invoke the active FAB directly. Version 0.2.3 changed defaults.
Build/static checks do not establish live behavior of changes made after device tests.

## Repository layout

- app/src/main/java/io/github/vibernoslop/: implementation.
- app/src/main/AndroidManifest.xml: legacy module metadata, icon, scope; no activities.
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
offline build. Existing output naming uses KurViber-VERSION.apk; changing it needs
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

All switches default to true. DEFAULT_TITLE is exactly KurViber✅. Previously saved user choices take
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

SettingsEntry inserts a KurViber section above settingsFragmentHolder while keeping
the holder ID and parent layout constraints. SettingsUi opens the module options dialog.
There is no MainActivity or launcher entry. Settings are available only inside Viber.

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

### Appearance (current scope)

All global color customization was removed at the owner's request in 0.4.0. Do not
restore accent fields, incoming/outgoing message colors, gradient flattening, contrast
palettes, blur bypasses, RenderEffect shaders or global resource/text color hooks.
The WHITE_HOME_ICONS option is the only color control and defaults to true.
HomeIconColors requires both HomeActivity and an ancestor resource entry name of
activity_home_toolbar, bottom_nav_bar or fab_container. More content, chats, group
settings, reactions, names, avatar borders and all other screens keep stock colors.
Toolbar ActionMenuItemView icons are compound drawables. FAB layer 0 is its stock
background; only foreground layers are whitened. Do not tint entire FAB composites.
The gear is white when the option is enabled and uses its original purple gradient
when disabled. Custom title and unrelated hiding/privacy switches remain available.
Old color preferences may remain in Viber storage but are ignored; do not clear Viber
data. Restart Viber to drop old in-memory hooks. HomeIconScopeCheck verifies exclusion
of unrelated screens/containers. Device visual verification is still needed.

### AI and obfuscation resistance

DexFinder is a small read-only DEX string-reference index with instruction/payload
width handling. ExperimentHooks locates the mapper using the diagnostic anchor
Too much time spent on mapping experiment= and validates a unique class/method shape.
ChatGPT keys and core_ai_writingassistant use null disabled results, verified nullable
in the analyzed APK. core_chats_imageremix must use its captured non-null Disabled
default, validated by DisabledExperimentDefault. Do not null arbitrary
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


## 0.4.0 removal of color experiments

The owner explicitly abandoned the 0.3.x global color experiments after repeated
visual failures. Scope is now ONLY white FAB glyphs, home toolbar icons and bottom
navigation icons through one switch; all other colors and gradients are stock.
Removed obsolete palette classes/tests and their UI fields, and NO_BLUR/FLAT/ACCENT
options. Do not carry forward historical defaults for outgoing/incoming colors.

## 0.4.1 search tint

Search still retained its gradient in 0.4.0. HomeControls now updates the actual
menu_search MenuItem icon tint and switches its mode from stock MULTIPLY to SRC_IN.
HomeIconColors handles both absolute and relative compound drawables and AppCompat
support compound tint. This remains scoped to the home toolbar and WHITE_HOME_ICONS.
Do not restore global color/resource hooks to fix one action-menu icon.

## 0.5.0 Polish message and Remix image switches

Owner supplied two screenshots on 2026-11-25. Polish message is the Writing Assistant
composer chip; Remix image is the CTA in the fullscreen media viewer. Additions use
independent POLISH_MESSAGE and IMAGE_REMIX options, both defaulting to true. They
are independent of AI/ChatGPT and TRANSLATE. SettingsUi renders Options automatically.

UiRules hides the inflated polish_messages_stub/polish_messages_menu and writing
assistant menu entry; never block ViewStub inflation itself. Image Remix hides the
gallery include view_image_remix_cta, its btn_image_remix_cta layout root, remix FAB
container, and inline remix button. Collapse only these controls, not fullscreen
gallery, media content, message composer, or ordinary edit/share actions.

AiFeatureRules recognizes only exact inspected experiment keys. Writing Assistant's
core_ai_writingassistant nullable disabled config suppresses its feature/triggers;
this toggle disables the Writing Assistant surface, not only an English text label.
Image Remix's core_chats_imageremix has a non-null sealed Disabled model. Its mapper
wrapper captures that default as an Object field. ExperimentHooks locates the wrapper
by Function1 interface; DisabledExperimentDefault validates a unique stateless singleton
whose toString is Disabled. Do not hardcode rdy/gvw or return null for Remix. On an
ambiguous/changed shape, skip the experiment override and retain UI hiding as fallback.
Do not override birthday/login auxiliary experiment models with guessed return types.

Validation: UI resource existence, independent-key policy checks, captured-default
identity/state/ambiguity tests, full APK build and signing. On-device testing of the
two screenshots' screens remains necessary. Screenshots/decompiled Viber inputs stay
outside the repository and source distributions.

## 0.5.1 actual Polish message action pill

Owner confirmed Remix hidden but Polish still present in 0.5.0. Runtime log showed
the Writing Assistant experiment overridden and old polish_messages_stub hidden. The
screenshot's actual Polish message text is wa_pill_polish_message, rendered separately
inside ChatActionPillsRowView (Compose), not the old polish_messages_menu.

WritingAssistantHooks intercepts setContent(List) on the retained row class (fallback:
unique public void(List) method). WritingAssistantPillFilter reads the unique no-arg
String getter from provider interfaces, irrespective of its obfuscated name, and
removes only providers with exact key ai_writing_assistant. Original source list is
not mutated; unrelated providers/order are retained; ambiguous or failing getters
are preserved. Do not hide chat_action_pills_row wholesale: it also hosts book_link.
The inspected Compose renderer emits no row padding/content for an empty list.

Local original APK remains 29.3.1; owner log's renamed classes show a different build.
Do not claim validation against their newer APK. Their advertising adapter hook also
reports a renamed isAdAvailable method; that separate issue is not fixed in 0.5.1.
Privacy and existing resource-based ad collapsing still run independently.

Important runtime markers: Polish: shared action-pill filter installed and Polish:
Writing Assistant pill provider removed. Unknown-provider counts are diagnostics only;
never log composer text or getter-return strings. Host tests cover old/renamed providers,
immutable input, independent disable switch, ambiguous/throwing getters, empty and
assistant-only lists. Actual phone validation remains necessary.


## 0.5.2 photo sender alignment after hiding Remix

MediaFooterAlignment observes only mediaDetailsContainer with imageRemixCtaButton
when IMAGE_REMIX is enabled. The sender belongs to descriptionView, an
ExpandableTextView whose internal text position depends on textMarginBottom.
After layout, derive that margin from the measured first-line center and the actual
visible reactionView/reactionViewExtended center in window coordinates. Never use a
fixed dp offset, translate the entire footer, or move the reaction control.

Only single-line descriptions are adjusted. Multiline captions retain the original
margin (restored when a reused widget changes from a name-only photo to a caption).
No message text is read or logged. Geometry tests cover font/density/reaction size,
window offsets and invalid measurements. APK build and host tests passed; the owner's
current Viber build still requires visual confirmation. Polish message is unchanged.


## 0.5.3 KurViber branding and settings labels

Application label, Viber settings entry and dialog, diagnostic report title and output
APK name use KurViber. Keep the package, Xposed entry point, preference names and signing
key unchanged. The application declares no activities; do not add launcher or framework
settings intents. Its icon is the owner-supplied image in res/drawable-nodpi/ic_module.jpg,
intentionally included as a public app asset. Original workspace image stays outside Git.

Switch labels are concise feature names without Hide/Disable prefixes. Settings groups
are Disable (checked removes the named feature) and Enable (More Gear Button, Search FAB
Action, White Home Icons). Preserve Boolean meanings and saved keys; do not invert any
preferences to unify presentation. All switches still default to true.
