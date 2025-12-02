package ch.onepass.onepass.model.payment

import ch.onepass.onepass.ui.payment.PaymentSheetResultHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

/**
 * Helper class to manage Stripe payment operations in OnePass.
 *
 * This class provides methods to handle payment flows using Stripe's Payment Sheet.
 * The PaymentSheet instance should be provided from MainActivity via CompositionLocal
 * to avoid lifecycle registration issues.
 *
 * Example usage in a Composable:
 * ```
 * val paymentSheet = LocalPaymentSheet.current
 * val stripeHelper = remember { StripePaymentHelper(paymentSheet) }
 *
 * // Present the payment sheet
 * stripeHelper.presentPaymentSheet(
 *     clientSecret = "pi_xxx_secret_xxx",
 *     onSuccess = { /* Handle successful payment */ },
 *     onCancelled = { /* Handle cancelled payment */ },
 *     onError = { error -> /* Handle error */ }
 * )
 * ```
 */
class StripePaymentHelper(
    private val paymentSheet: PaymentSheet?
) {

    private var onSuccessCallback: (() -> Unit)? = null
    private var onCancelledCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    /**
     * Secondary constructor for backwards compatibility.
     * Creates the helper without a PaymentSheet (will fail when trying to present).
     */
    constructor() : this(null)

    /**
     * Constructor that initializes with an activity (for backwards compatibility).
     * Note: This may cause lifecycle issues if called during composition.
     * Prefer using the constructor that takes a PaymentSheet instance.
     */
    constructor(activity: androidx.activity.ComponentActivity) : this(
        PaymentSheet(activity) { result ->
            // Result handling is done through PaymentSheetResultHandler
            PaymentSheetResultHandler.handleResult(result)
        }
    )

    init {
        // Register this instance to handle PaymentSheet results
        if (paymentSheet != null) {
            PaymentSheetResultHandler.setHandler { result ->
                handlePaymentSheetResult(result)
            }
        }
    }

    /**
     * Returns true if the PaymentSheet has been initialized.
     */
    val isInitialized: Boolean
        get() = paymentSheet != null

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
        if (paymentSheet == null) {
            onError("PaymentSheet not initialized. Please ensure Stripe is configured.")
            return
        }

        // Register callbacks before presenting
        this.onSuccessCallback = onSuccess
        this.onCancelledCallback = onCancelled
        this.onErrorCallback = onError

        // Register this instance as the result handler
        PaymentSheetResultHandler.setHandler { result ->
            handlePaymentSheetResult(result)
        }

        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "OnePass",
            customer = customerConfig,
            allowsDelayedPaymentMethods = true
        )

        paymentSheet.presentWithPaymentIntent(
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
