# Third-party software

The included ARM64 `libskr_llama.so` statically links llama.cpp and ggml from llama.cpp tag b6323. Their MIT license and third-party notices are included under `prebuilt/`.
Android / Kotlin dependencies are resolved by Gradle and retain their upstream licenses. Qwen weights are not bundled; review the exact model publisher's license before downloading or redistributing weights.

CodesbyFebin branding is used as requested. The app is not an official Solana Mobile store client or Guardian service.

RepoStore APK ABI filename patterns and token matching are adapted from Samyak Kamble’s MIT-licensed RepoStore. The complete notice is retained in `third_party/RepoStore/LICENSE`. Package observation and update flow follow its design, implemented for Compose / encrypted storage. No SafeHaven code is included.
