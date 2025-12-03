package ch.onepass.onepass.ui.payment

import androidx.activity.ComponentActivity
import androidx.compose.runtime.compositionLocalOf
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

/**
 * CompositionLocal to provide PaymentSheet instance throughout the app. This ensures PaymentSheet
 * is initialized in MainActivity.onCreate before the Activity reaches RESUMED state, avoiding
 * lifecycle registration issues.
 */
val LocalPaymentSheet = compositionLocalOf<PaymentSheet?> { null }

/**
 * Creates a PaymentSheet instance for the given activity. This should be called in
 * MainActivity.onCreate to ensure proper lifecycle timing.
 *
 * The result callback is handled by a global handler that forwards to the current
 * StripePaymentHelper instance.
 */
fun createPaymentSheet(activity: ComponentActivity): PaymentSheet {
  return PaymentSheet(activity) { result ->
    // Result handling is forwarded to the current StripePaymentHelper instance
    // via the global result handler
    PaymentSheetResultHandler.handleResult(result)
  }
}

/**
 * Global handler for PaymentSheet results. This is needed because PaymentSheet's result callback is
 * set in the constructor, but we want to handle results in the composable where StripePaymentHelper
 * is used.
 */
object PaymentSheetResultHandler {
  private var currentHandler: ((PaymentSheetResult) -> Unit)? = null

  fun setHandler(handler: (PaymentSheetResult) -> Unit) {
    currentHandler = handler
  }

  fun clearHandler() {
    currentHandler = null
  }

  internal fun handleResult(result: PaymentSheetResult) {
    currentHandler?.invoke(result)
  }
}
