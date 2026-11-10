package com.bloodbridge.domain;

public enum BloodGroup {
    A_POS("A+", "A", true), A_NEG("A-", "A", false),
    B_POS("B+", "B", true), B_NEG("B-", "B", false),
    AB_POS("AB+", "AB", true), AB_NEG("AB-", "AB", false),
    O_POS("O+", "O", true), O_NEG("O-", "O", false);

    private final String label;
    private final String abo;
    private final boolean rhPositive;

    BloodGroup(String label, String abo, boolean rhPositive) {
        this.label = label;
        this.abo = abo;
        this.rhPositive = rhPositive;
    }

    public String label() { return label; }
    public String abo() { return abo; }
    public boolean rhPositive() { return rhPositive; }
}
