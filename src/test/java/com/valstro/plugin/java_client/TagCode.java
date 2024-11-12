package com.valstro.plugin.java_client;

public enum TagCode {
    WAB,
    NR,
    NP,
    ET,
    NOCAT,
    PRE,
    AFT,
    DNI,
    DNR,
    VWAP,
    CNT,
    R4,
    MNP,
    NCP,
    UX,
    CCPE,
    AFXO,
    TAOK,
    MM;

    public com.valstro.plugin.generated.TagCode asTagCode() {
        return com.valstro.plugin.generated.TagCode.valueOf(this.name());
    }
}
