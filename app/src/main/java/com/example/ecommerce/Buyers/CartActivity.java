package com.example.ecommerce.Buyers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ecommerce.Model.Cart;
import com.example.ecommerce.Model.Products;
import com.example.ecommerce.Prevalent.Prevalent;
import com.example.ecommerce.R;
import com.example.ecommerce.ViewHolder.CartViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity
{
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private Button NextProcessBtn;
    private TextView txtTotalAmount, txtMsg1;
    private ArrayList<Cart> prod=new ArrayList<>();
private Cart c;
    private int overTotalPrice = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.cart_list);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        NextProcessBtn = (Button) findViewById(R.id.next_process_btn);
        txtTotalAmount = (TextView) findViewById(R.id.page_title);
        txtMsg1 = (TextView) findViewById(R.id.msg1);

        NextProcessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                txtTotalAmount.setText("Total Price = " + String.valueOf(overTotalPrice));
                Log.d("overtotalprice",overTotalPrice+"");
               Intent intent = new Intent(CartActivity.this, ConfirmFinalOrderActivity.class);
               intent.putExtra("Total Price", String.valueOf(overTotalPrice));
             intent.putExtra("cart",prod);
               startActivity(intent);
               finish();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        CheckOrderState();

        final DatabaseReference cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");

        FirebaseRecyclerOptions<Cart> options =
                new FirebaseRecyclerOptions.Builder<Cart>()
                .setQuery(cartListRef.child("User View")
                .child(Prevalent.currentOnlineUsers.getPhone())
                        .child("Products"), Cart.class)
                        .build();

       prod.clear();
        FirebaseRecyclerAdapter<Cart, CartViewHolder> adapter
                = new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull final Cart model)
            {
     //   c=model
                prod.add(model);
                holder.txtProductQuantity.setText("Quantity = " + model.getQuantity());
                 holder.txtProductPrice.setText("Price " + model.getPrice());
                 holder.txtProductName.setText(model.getPname());
Log.d("pricemoney",model.getPrice());

                 int oneTypeProductTPrice = ((Integer.valueOf((model.getPrice())))) * Integer.valueOf(model.getQuantity());
Log.d("onetypeproduct",oneTypeProductTPrice+"");
                 overTotalPrice = overTotalPrice + oneTypeProductTPrice;

                 holder.itemView.setOnClickListener(new View.OnClickListener() {
                     @Override
                     public void onClick(View view)
                     {
                         CharSequence options[] = new CharSequence[]
                                 {
                                   "Edit",
                                         "Remove"
                                 };
                         AlertDialog.Builder builder = new AlertDialog.Builder(CartActivity.this);
                         builder.setTitle("Cart Options:");

                         builder.setItems(options, new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int i)
                             {
                                 if(i == 0)
                                 {
                                     Intent intent = new Intent(CartActivity.this, ProductDetailsActivity.class);
                                     intent.putExtra("pid", model.getPid());
                                     startActivity(intent);
                                 }
                                 if(i == 1)
                                 {
                                     cartListRef.child("User View")
                                             .child(Prevalent.currentOnlineUsers.getPhone())
                                             .child("Products")
                                             .child(model.getPid())
                                             .removeValue()
                                             .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                 @Override
                                                 public void onComplete(@NonNull Task<Void> task)
                                                 {
                                                     if(task.isSuccessful())
                                                     {
                                                         Toast.makeText(CartActivity.this, "Item removed successfully.", Toast.LENGTH_SHORT).show();

                                                         Intent intent = new Intent(CartActivity.this, HomeActivity.class);
                                                         startActivity(intent);
                                                     }
                                                 }
                                             });
                                 }
                             }
                         });
                         builder.show();
                     }
                 });
            }

            @NonNull
            @Override
            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_items_layout, parent, false);
                CartViewHolder holder = new CartViewHolder(view);
                return holder;
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void CheckOrderState()
    {
        DatabaseReference ordersRef;
        ordersRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(Prevalent.currentOnlineUsers.getPhone());
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
               if (dataSnapshot.exists())
               {
                   String shippingState = dataSnapshot.child("state").getValue().toString();
                   String userName = dataSnapshot.child("pname").getValue().toString();

                   if(shippingState.equals("shipped"))
                   {
                       txtTotalAmount.setText("Dear " + userName + "\n order is shipped successfully.");
                       recyclerView.setVisibility(View.GONE);

                       txtMsg1.setVisibility(View.VISIBLE);
                       txtMsg1.setText("Congratulations, your final order has been Shipped successfully. Soon you will receive your order at your home address.");
                       NextProcessBtn.setVisibility(View.GONE);

                       Toast.makeText(CartActivity.this, "you can purchase more products, once you receive your first final order.", Toast.LENGTH_SHORT).show();
                   }
                   else if (shippingState.equals("not shipped"))
                   {
                       txtTotalAmount.setText("Shipping state = Not Shipped");
                       recyclerView.setVisibility(View.GONE);

                       txtMsg1.setVisibility(View.VISIBLE);
                       NextProcessBtn.setVisibility(View.GONE);

                       Toast.makeText(CartActivity.this, "you can purchase more products, once you receive your first final order.", Toast.LENGTH_SHORT).show();
                   }
               }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String removeLastDigit(String price) {

        return  price.substring(0, price.length() - 1);
    }
}
