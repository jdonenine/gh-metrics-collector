package com.onenine.ghmc.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkedPull {
    private String org;
    private String repo;
    private Integer number;
    private LinkedPullType type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedPull that = (LinkedPull) o;
        return org.equals(that.org) && repo.equals(that.repo) && number.equals(that.number) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(org, repo, number, type);
    }
}
