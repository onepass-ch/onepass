package ch.onepass.onepass.model.payment

import android.app.Activity
import androidx.activity.ComponentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

/**
 * Helper class to manage Stripe payment operations in OnePass.
 * 
 * This class provides methods to handle payment flows using Stripe's Payment Sheet.
 * 
 * Example usage:
 * ```
 * val stripeHelper = StripePaymentHelper(activity)
 * stripeHelper.presentPaymentSheet(
 *     clientSecret = "pi_xxx_secret_xxx",
 *     onSuccess = { /* Handle successful payment */ },
 *     onCancelled = { /* Handle cancelled payment */ },
 *     onError = { error -> /* Handle error */ }
 * )
 * ```
 */
class StripePaymentHelper(private val activity: ComponentActivity) {

    private var paymentSheet: PaymentSheet? = null

    init {
        // Initialize PaymentSheet
        paymentSheet = PaymentSheet(activity) { result ->
            handlePaymentSheetResult(result)
        }
    }

    private var onSuccessCallback: (() -> Unit)? = null
    private var onCancelledCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    /**
     * Presents the Stripe Payment Sheet to the user.
     * 
     * @param clientSecret The client secret from your backend (from PaymentIntent or SetupIntent)
     * @param customerConfig Optional customer configuration for saved payment methods
     * @param onSuccess Callback invoked when payment succeeds
     * @param onCancelled Callback invoked when user cancels the payment
     * @param onError Callback invoked when an error occurs
     */
    fun presentPaymentSheet(
        clientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration? = null,
        onSuccess: () -> Unit,
        onCancelled: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        this.onSuccessCallback = onSuccess
        this.onCancelledCallback = onCancelled
        this.onErrorCallback = onError

        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "OnePass",
            customer = customerConfig,
            allowsDelayedPaymentMethods = true
        )

        paymentSheet?.presentWithPaymentIntent(
            paymentIntentClientSecret = clientSecret,
            configuration = configuration
        )
    }

    /**
     * Handles the result from the Payment Sheet.
     */
    private fun handlePaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                // Payment successful
                onSuccessCallback?.invoke()
            }
            is PaymentSheetResult.Canceled -> {
                // User cancelled the payment
                onCancelledCallback?.invoke()
            }
            is PaymentSheetResult.Failed -> {
                // Payment failed
                onErrorCallback?.invoke(result.error.message ?: "Payment failed")
            }
        }
    }

    /**
     * Creates a customer configuration for returning customers.
     * This allows users to see and select their saved payment methods.
     * 
     * @param customerId The Stripe customer ID from your backend
     * @param ephemeralKey The ephemeral key from your backend
     */
    fun createCustomerConfiguration(
        customerId: String,
        ephemeralKey: String
    ): PaymentSheet.CustomerConfiguration {
        return PaymentSheet.CustomerConfiguration(
            id = customerId,
            ephemeralKeySecret = ephemeralKey
        )
    }
}
