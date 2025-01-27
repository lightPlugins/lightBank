package io.lightstudios.bank.api.models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BankLevel {

    private int level;
    private BigDecimal maxBalance;
}
