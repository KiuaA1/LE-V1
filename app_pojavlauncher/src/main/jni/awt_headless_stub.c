/*
 * Empty native companion for libawt_headless.
 * The legacy ndk-build project intentionally produced an empty shared library;
 * keep that ABI surface while using CMake.
 */
void le_awt_headless_stub(void) {}
