package dev.forcetower.hilt.android.dynamic.processor.internal;

import dagger.Component;
import dev.forcetower.hilt.android.dynamic.codegen.kotlin.KotlinMetadataUtil;

import javax.inject.Singleton;

/**
 * A single-use provider of {@link KotlinMetadataUtil}. Since the returned util has a cache, it is
 * better to reuse the same instance as much as possible, except for going across processor rounds
 * because the cache contains elements.
 */
// TODO(erichang):  Revert this, should be wrapped with a Dagger module.
public final class KotlinMetadataUtils {

  @Singleton
  @Component
  interface MetadataComponent {
    KotlinMetadataUtil get();
  }

  /** Gets the metadata util. */
  public static KotlinMetadataUtil getMetadataUtil() {
    return DaggerKotlinMetadataUtils_MetadataComponent.create().get();
  }

  private KotlinMetadataUtils() {}
}
