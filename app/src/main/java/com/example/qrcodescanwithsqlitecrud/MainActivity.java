package com.example.qrcodescanwithsqlitecrud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ProductAdapter.Edit {
    RecyclerView recyclerView;
    List<Product> productList;
    SQLiteOpenHelper openHelper;
    TextView textView;
    Cursor cursor;

    private double calculateTotal() {
        double total = 0;
        for (Product p: productList) {
            total += (p.getProductPrice()*p.getProductQuantity());
        }
        return total;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openHelper = new DatabaseHelper(this);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.INTERNET}, PackageManager.PERMISSION_GRANTED);

        recyclerView = findViewById(R.id.recycler_view);

        productList = new ArrayList<>();

        recyclerView.setAdapter(new ProductAdapter(productList));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textView = findViewById(R.id.total);
        textView.setText(String.format(Locale.getDefault(),"Total: Rs. %.2f",calculateTotal()));

        registerForContextMenu(recyclerView);
    }

    public void scanButton(View v) {
        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
        intentIntegrator.initiateScan();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult resultIntent = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (resultIntent != null) {
            if (resultIntent.getContents()!= null) {
                cursor = openHelper.getReadableDatabase().rawQuery(
                        "SELECT * FROM "+DatabaseHelper.TABLE_NAME
                        +" WHERE " +
                        DatabaseHelper.COL_1+"="+Integer.parseInt(resultIntent.getContents()),
                        null);

                if(cursor.getCount()!= 0) {
                    while(cursor.moveToNext()) {
                        productList.add(
                                new Product(
                                        cursor.getInt(0),
                                        cursor.getString(1),
                                        1,
                                        cursor.getString(4),
                                        cursor.getDouble(3)
                                )
                        );
                    }

                    textView.setText(String.format(Locale.getDefault(),"Total: Rs. %.2f",calculateTotal()));

                    recyclerView.setAdapter(new ProductAdapter(productList));
                }
            }
            else {
                System.out.println("Content is null");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void editData(int position) {
        Product product = productList.get(position);

        EditData editData = new EditData();
        EditData.DataListener dataListener = () -> {
            Bundle bundle = new Bundle();
            bundle.putInt("position",position);
            bundle.putInt("barcode",product.getProductID());
            bundle.putString("name",product.getProductName());
            bundle.putDouble("price",product.getProductPrice());
            bundle.putString("URL",product.getProductImageURL());
            bundle.putInt("quantity",product.getProductQuantity());
            return bundle;
        };

        editData.setDataListener(dataListener);
        editData.show(getSupportFragmentManager(),"Edit");

        EditData.PassData getUpdatedDataListener = (bundle) -> {
            Product product1 = productList.get(bundle.getInt("position"));
            product1.setProductQuantity(bundle.getInt("quantity"));
            productList.set(bundle.getInt("position"), product1);

            textView.setText(String.format(Locale.getDefault(),"Total: Rs. %.2f",calculateTotal()));
            recyclerView.setAdapter(new ProductAdapter(productList));
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        };
        editData.setPassDataListener(getUpdatedDataListener);
    }
}