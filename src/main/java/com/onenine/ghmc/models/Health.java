package com.onenine.ghmc.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Health {
    private boolean live = false;
    private boolean ready = false;
}
