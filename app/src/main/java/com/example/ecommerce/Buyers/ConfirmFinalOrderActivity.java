package com.example.ecommerce.Buyers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ecommerce.Config;
import com.example.ecommerce.Model.Cart;
import com.example.ecommerce.Model.PaymentVerificationResponse;
import com.example.ecommerce.PayPalAPI;
import com.example.ecommerce.Prevalent.Prevalent;
import com.example.ecommerce.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalItem;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalPaymentDetails;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import butterknife.BindString;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConfirmFinalOrderActivity extends AppCompatActivity
{  private static final String CONFIG_ENVIRONMENT = PayPalConfiguration.ENVIRONMENT_SANDBOX;
    private static final int REQUEST_CODE_PAYMENT = 1;
ArrayList<Cart> prod=new ArrayList<>();
private String TAG="CHECKING";
    private static PayPalConfiguration paypalConfig = new PayPalConfiguration()
            .environment(CONFIG_ENVIRONMENT)
            .clientId(Config.CONFIG_CLIENT_ID);
    private EditText nameEditText, phoneEditText, addressEditText, cityEditText;
    private Button confirmOrderBtn;
    private List<PayPalItem> payPalItems = new ArrayList<PayPalItem>();
    // Progress dialog
    private ProgressDialog pDialog;
    Retrofit retrofit;
    PayPalAPI service;
    @BindString(R.string.local_host)String local_host;
    private String totalAmount = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_final_order);

        totalAmount = getIntent().getStringExtra("Total Price");

        prod=(ArrayList<Cart>)(getIntent().getSerializableExtra("cart"));

        Toast.makeText(this, "Total Price = " + totalAmount, Toast.LENGTH_SHORT).show();


        build_API_Service();

        startPayPalService();

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
        confirmOrderBtn = (Button) findViewById(R.id.confirm_final_order_btn);
        phoneEditText = (EditText) findViewById(R.id.shippment_phone_number);
        nameEditText = (EditText) findViewById(R.id.shippment_name);
        addressEditText = (EditText) findViewById(R.id.shippment_address);
        cityEditText = (EditText) findViewById(R.id.shippment_city);

        confirmOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                OnBuyClicked();
            }
        });

    }
    private void build_API_Service() {
        retrofit = new Retrofit.Builder()
                .baseUrl("https://limitless-savannah-39443.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(PayPalAPI.class);
    }

    private void startPayPalService(){
        // Starting PayPal service
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, paypalConfig);
        startService(intent);
    }

    public void OnBuyClicked() {

        // Check for empty cart
        if (prod.size() > 0) {
            // check if paypal Items list has products ?!
            // if so then clear it to avoid duplication
            if(!payPalItems.isEmpty())
                payPalItems.clear();

            // adding shopping cart items to paypal
            for(int i=0;i<prod.size();i++){
              //  PayPalItem h=new PayPalItem()
                PayPalItem item = new PayPalItem(prod.get(i).getPname()
                        , Integer.parseInt(prod.get(i).getQuantity())
                        , BigDecimal.valueOf(Double.parseDouble(prod.get(i).getPrice()))
                        , Config.DEFAULT_CURRENCY
                        , prod.get(i).getPid());

                payPalItems.add(item);
            }

            launchPayPalPayment();

        }
        else {
            startActivity(new Intent(this,HomeActivity.class));
        }
    }


    /**
     * Launching PalPay payment activity to complete the payment
     * */
    private void launchPayPalPayment() {

        PayPalPayment thingsToBuy = prepareFinalCart();

        Intent intent = new Intent(this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, paypalConfig);

        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingsToBuy);

        startActivityForResult(intent, REQUEST_CODE_PAYMENT);
    }
    private PayPalPayment prepareFinalCart() {

        PayPalItem[] items = new PayPalItem[payPalItems.size()];
        items = payPalItems.toArray(items);

        // Total amount
        BigDecimal subtotal = PayPalItem.getItemTotal(items);
Log.d("sub_total",subtotal.toString());
        // If you have shipping cost, add it here
        BigDecimal shipping = new BigDecimal("0.0");

        // If you have tax, add it here
        BigDecimal tax = new BigDecimal("0.0");

        PayPalPaymentDetails paymentDetails = new PayPalPaymentDetails(
                shipping, subtotal, tax);

        BigDecimal amount = subtotal.add(shipping).add(tax);

        PayPalPayment payment = new PayPalPayment(
                amount,
                Config.DEFAULT_CURRENCY,
                "we are happy to serve.",
                PayPalPayment.PAYMENT_INTENT_SALE);

        payment.items(items).paymentDetails(paymentDetails);

        // Custom field like invoice_number etc.,
        payment.custom("This is text that will be associated with the payment that the app can use.");

        return payment;
    }


    private void verifyPaymentOnServer(final String paymentId , PaymentConfirmation confirmation ){
        // Showing progress dialog before making request
        pDialog.setMessage("Verifying payment...");
        showpDialog();

        try {
            String amount = confirmation.getPayment().toJSONObject().getString("amount");
            String currency = confirmation.getPayment().toJSONObject().getString("currency_code");
            String userID = "";
            if(FirebaseAuth.getInstance()!=null){
                userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            service.VerifyMobilePayment(paymentId ,userID, amount , currency ).enqueue(new Callback<PaymentVerificationResponse>() {
                @Override
                public void onResponse(Call<PaymentVerificationResponse> call, Response<PaymentVerificationResponse> response) {
                    // hiding the progress dialog
                    hidepDialog();
                    Log.d("msg" , response.body().getMsg());
                    Log.d("state" , response.body().getMsg());
                }

                @Override
                public void onFailure(Call<PaymentVerificationResponse> call, Throwable e) {
                    // hiding the progress dialog
                    hidepDialog();
                    Log.e("error " , e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void showpDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hidepDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm =
                        data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirm != null) {
                    try {
                        Log.i(TAG, confirm.toJSONObject().toString(4));
                        Log.i(TAG, confirm.getPayment().toJSONObject().toString(4));
                        /**
                         *  TODO: send 'confirm' (and possibly confirm.getPayment() to your server for verification
                         * or consent completion.
                         * See https://developer.paypal.com/webapps/developer/docs/integration/mobile/verify-mobile-payment/
                         * for more details.
                         *
                         * For sample mobile backend interactions, see
                         * https://github.com/paypal/rest-api-sdk-python/tree/master/samples/mobile_backend
                         */

                        String payment_id = confirm.toJSONObject().getJSONObject("response").getString("id");

                        verifyPaymentOnServer(payment_id , confirm);

                        displayResultText("PaymentConfirmation info received from PayPal");

                        Check();

                    } catch (JSONException e) {
                        Log.e(TAG, "an extremely unlikely failure occurred: ", e);
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i(TAG, "The user canceled.");
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Log.i(
                        TAG,
                        "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
            }
        }
    }

    protected void displayResultText(String result) {
        Toast.makeText(
                getApplicationContext(),
                result, Toast.LENGTH_LONG)
                .show();
    }

    private void Check()
    {
        if (TextUtils.isEmpty(nameEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your full name.", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your phone number.", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(addressEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your address.", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(cityEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your city name.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            ConfirmOrder();
        }
    }

    private void ConfirmOrder()
    {
        final String saveCurrentDate, saveCurrentTime;

        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentDate.format(calForDate.getTime());

        final DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                .child("Orders")
                .child(Prevalent.currentOnlineUsers.getPhone());

        HashMap<String, Object> ordersMap = new HashMap<>();
        ordersMap.put("totalAmount", totalAmount);
        ordersMap.put("pname", nameEditText.getText().toString());
        ordersMap.put("phone", phoneEditText.getText().toString());
        ordersMap.put("address", addressEditText.getText().toString());
        ordersMap.put("city", cityEditText.getText().toString());
        ordersMap.put("date", saveCurrentDate);
        ordersMap.put("time", saveCurrentTime);
        ordersMap.put("state", "not shipped");

        ordersRef.updateChildren(ordersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
               if (task.isSuccessful())
               {
                   FirebaseDatabase.getInstance().getReference()
                           .child("Cart List")
                           .child("User View")
                           .child(Prevalent.currentOnlineUsers.getPhone())
                           .removeValue()
                           .addOnCompleteListener(new OnCompleteListener<Void>() {
                               @Override
                               public void onComplete(@NonNull Task<Void> task)
                               {
                                  if (task.isSuccessful())
                                  {
                                      Toast.makeText(ConfirmFinalOrderActivity.this, "your final order has been placed successfully.", Toast.LENGTH_SHORT).show();

                                      Intent intent = new Intent(ConfirmFinalOrderActivity.this, HomeActivity.class);
                                      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                      startActivity(intent);
                                      finish();

                                  }
                               }
                           });
               }
            }
        });
    }
}
