package net.azterisk.weighttracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    public static final String PRODUCT_ID_REMOVE_ADS = "remove_ads_premium";
    public static final String KEY_IS_PREMIUM = "is_premium_unlocked";

    private final Context context;
    private BillingClient billingClient;
    private ProductDetails premiumProductDetails;
    private OnPurchaseUpdatedListener listener;

    public interface OnPurchaseUpdatedListener {
        void onPremiumStatusChanged(boolean isPremium);
    }

    public BillingManager(Context context, OnPurchaseUpdatedListener listener) {
        this.context = context;
        this.listener = listener;
        initBillingClient();
    }

    private void initBillingClient() {
        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();

        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .build();

        startConnection();
    }

    public void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryPurchases();
                    queryProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Retry connection
            }
        });
    }

    public boolean isPremiumUser() {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    private void setPremiumUser(boolean isPremium) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
        if (listener != null) {
            listener.onPremiumStatusChanged(isPremium);
        }
    }

    private void queryPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        boolean hasPremium = false;
                        for (Purchase purchase : purchases) {
                            if (purchase.getProducts().contains(PRODUCT_ID_REMOVE_ADS) &&
                                    purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                hasPremium = true;
                                break;
                            }
                        }
                        setPremiumUser(hasPremium);
                    }
                }
        );
    }

    private void queryProductDetails() {
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && result != null) {
                List<ProductDetails> detailsList = result.getProductDetailsList();
                if (detailsList != null && !detailsList.isEmpty()) {
                    premiumProductDetails = detailsList.get(0);
                }
            }
        });
    }

    public void purchaseRemoveAds(Activity activity) {
        if (isPremiumUser()) {
            Toast.makeText(context, "Ad-Free Premium Mode is already unlocked!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (premiumProductDetails != null) {
            BillingFlowParams.ProductDetailsParams productDetailsParams =
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(premiumProductDetails)
                            .build();

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(productDetailsParams))
                    .build();

            billingClient.launchBillingFlow(activity, billingFlowParams);
        } else {
            // Test Mode for simulation before product publishing on Google Play Console
            setPremiumUser(true);
            Toast.makeText(context, "Ad-Free Premium Mode Unlocked (Test Mode)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains(PRODUCT_ID_REMOVE_ADS)) {
                    setPremiumUser(true);
                    Toast.makeText(context, "Purchase Successful! Ads Removed.", Toast.LENGTH_LONG).show();
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(context, "Purchase canceled.", Toast.LENGTH_SHORT).show();
        }
    }
}
