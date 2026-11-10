package __PKG__;

import androidx.core.content.FileProvider;

// Own subclass so it doesn't clash with Capacitor's FileProvider entry in the manifest.
public class BruceFileProvider extends FileProvider {}
