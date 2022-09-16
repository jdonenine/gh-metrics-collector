package com.onenine.ghmc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "sync-#{@environment.getProperty('tenant.id')}")
public class Sync {
    @Id
    private String id;
    @Field(type = FieldType.Date)
    private ZonedDateTime timestamp;
    private String org;
    private String repo;
    private PullDateType pullDateType;
    private String afterDate;
    private PullState pullState;

    public Sync(ZonedDateTime timestamp, String org, String repo, PullState pullState, PullDateType pullDateType, String afterDate) {
        this.timestamp = timestamp;
        this.org = org;
        this.repo = repo;
        this.pullDateType = pullDateType;
        this.afterDate = afterDate;
        this.pullState = pullState;
        this.id = buildId();
    }

    @JsonIgnore
    private String buildId() throws IllegalArgumentException {
        if (org == null || org.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (repo == null || repo.isEmpty()) {
            throw new IllegalArgumentException("org is required");
        }
        if (pullDateType == null) {
            throw new IllegalArgumentException("pullDateType is required");
        }
        if (afterDate == null || afterDate.isEmpty()) {
            throw new IllegalArgumentException("afterDate is required");
        }
        if (pullState == null) {
            throw new IllegalArgumentException("pullState is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        return org + "|" + repo + "|" + pullState.name() + pullDateType.name() + "|" + afterDate + "|" + timestamp.toEpochSecond();
    }
}
