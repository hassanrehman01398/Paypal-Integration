package com.example.ecommerce.Model;

import java.util.ArrayList;

/**
 * Created by Omar on 7/9/2017.
 */

public class ProductsResponse {

    ArrayList<Products>products = new ArrayList<>();
    int status;

    public ArrayList<Products> getProducts() {
        return products;
    }

    public int getStatus() {
        return status;
    }
}
