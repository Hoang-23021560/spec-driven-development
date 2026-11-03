package com.bunledger.model;

import lombok.Getter;

@Getter
public enum TransactionType {
    INCOME("Khoản Thu"),
    EXPENSE("Khoản Chi");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }
}

