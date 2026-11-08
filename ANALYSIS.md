# Target notes: 29.3.1.0 / 1290301

Inputs are kept outside the module project: base.apk plus arm64-v8a and xxhdpi splits.
Screenshots show ads in Chats/More, folder tabs, ChatGPT/lower navigation, inline Translate.

Verified against decompiled resources/code:

- `PreferencesWithAdsRecyclerAdapterWrapper` → superclass `v3d0`.
  `isAdAvailable()` feeds `getItemCount()`, `getItemId()`, `getItemViewType()` and
  position mapping. `mgb` is the Chats subclass and calls the base availability check.
- `RecyclerView.W(View): Rect` computes item-decoration insets. Match by unique
  signature rather than `W` or the original AndroidX name.
- `BottomNavigationView` uses ordinary LinearLayout children, assigned stable resource
  entry IDs. Preserve its internal arrays and indices; hide individual child views.
- `activity_home.xml`: main content bottom is constrained to `bottom_nav_bar`, so GONE
  plus zero size permits the content to occupy the former bar region.
- `activity_settings.xml`: `settingsFragmentHolder` is a FrameLayout below the toolbar.
  Wrap it with a section row, retain its ID and the original parent layout parameters.
- `msg_list_translate_button`: concrete LinearLayout inflated by a ViewStub. Leave
  ViewStub visibility semantics alone; collapse the inflated button, not the stub.
- `pvw.c(vvw, boolean): Object`: unique diagnostic string
  `Too much time spent on mapping experiment=`. Holds experiment descriptor `hxw`
  whose String field is the experiment key. Three keys have nullable disabled states:
  `core_ai_chatgptintegration`, `core_ai_chatgpttab`, `core_ai_chatgpttabv2`.
- `DataSdkGbExperimentProvider.SerializableModel`: two Boolean fields represent
  `directOutput` and `dataSdkOutput`. Force both off on model construction. The
  dispatcher is `zja` in this build; no hardcoded hook uses that obfuscated name.
- `Rakuten AdUnit.c(OnCompleteListener)`: bid request entry point. One void method with
  that callback signature. Return NO_BIDS through the existing callback on the main queue.

The decompiler may fail on unrelated large methods. Decompiled sources and user's
screenshots are analysis inputs, not shipped in the module or source archive.

Additional SDK obfuscation in 29.3.1:

- `Setting Braze SDK disabled to: ` → `bo.app.fg`; sole instance `boolean()` reads
  the Braze disabled state. Override its result without persisting SDK preferences.
- `Crashlytics automatic data collection ` → `kkh`; sole instance `boolean()`
  controls automatic collection, including existing preference overrides of metadata.
- `Failed to load ad.` → `os` and a synthetic runnable `wlu`. Only `os` has both
  Context and IInterface instance fields plus the one-argument request method.
  Its request type `sd81` is wrapped by `au`, the argument of `BaseAdView.b(au)`.
- Stable public AdLoader/Braze class names alone do not work in this APK. Their legacy
  hooks remain optional fallback paths, and the DEX discovery is the current path.
