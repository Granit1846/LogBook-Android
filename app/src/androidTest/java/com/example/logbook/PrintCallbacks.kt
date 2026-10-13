// Test-only bridges: Android callback constructors have package visibility.
package android.print
abstract class TestLayoutCallback : PrintDocumentAdapter.LayoutResultCallback()
abstract class TestWriteCallback : PrintDocumentAdapter.WriteResultCallback()
