package com.sam_chordas.android.stockhawk.exceptions;

public class StockNotFoundException extends RuntimeException{

    public StockNotFoundException(String message){
        super(message);
    }
}
