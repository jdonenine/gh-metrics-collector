package com.onenine.ghmc.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

import static org.slf4j.LoggerFactory.getLogger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullComment implements Comparable<PullComment> {
    private static final Logger log = getLogger(PullComment.class);

    private String org;
    private String repo;
    private Integer number;
    private String user;
    @Field(type = FieldType.Date)
    private ZonedDateTime createdAt;
    private boolean isReviewComment;

    @Override
    public int compareTo(PullComment that) {
        ZonedDateTime thisCreatedAt = getCreatedAt();
        ZonedDateTime thatCreatedAt = that != null ? that.getCreatedAt() : null;
        if (thisCreatedAt == null && thatCreatedAt == null) {
            return 0;
        }
        if (thisCreatedAt != null && thatCreatedAt == null) {
            return -1;
        }
        if (thisCreatedAt == null && thatCreatedAt != null) {
            return 1;
        }
        return thisCreatedAt.compareTo(thatCreatedAt);
    }
}
