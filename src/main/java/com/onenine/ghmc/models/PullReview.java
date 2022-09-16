package com.onenine.ghmc.models;

import com.onenine.ghmc.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewState;
import org.slf4j.Logger;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

import static org.slf4j.LoggerFactory.getLogger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullReview implements Comparable<PullReview> {
    private static final Logger log = getLogger(PullReview.class);

    private String org;
    private String repo;
    private Integer number;
    private String user;
    @Field(type = FieldType.Date)
    private ZonedDateTime submittedAt;
    private boolean approved;
    private boolean changesRequested;

    public static PullReview fromGHPullRequestReview(String org, String repo, int number, GHPullRequestReview ghReview) {
        if (ghReview == null) {
            return null;
        }

        PullReview review = new PullReview();
        review.setOrg(org);
        review.setRepo(org);
        review.setNumber(number);

        try {
            review.setSubmittedAt(TimeUtils.getUtcDateTimeFromDate(ghReview.getSubmittedAt()));
        } catch (Exception e) {
            log.warn("Unable to retrieve submittedAt for pull request review {}/{}/{}/{}", org, repo, number, ghReview.getId());
        }

        try {
            review.setUser(ghReview.getUser().getLogin());
        } catch (Exception e) {
            log.warn("Unable to retrieve user for pull request review {}/{}/{}/{}", org, repo, number, ghReview.getId());
        }

        review.setApproved(ghReview.getState() != null ? ghReview.getState().equals(GHPullRequestReviewState.APPROVED) : false);

        review.setChangesRequested(ghReview.getState() != null ? ghReview.getState().equals(GHPullRequestReviewState.CHANGES_REQUESTED) : false);

        return review;
    }

    @Override
    public int compareTo(PullReview that) {
        ZonedDateTime thisSubmittedAt = this.getSubmittedAt();
        ZonedDateTime thatSubmittedAt = that != null ? that.getSubmittedAt() : null;
        if (thisSubmittedAt == null && thatSubmittedAt == null) {
            return 0;
        }
        if (thisSubmittedAt != null && thatSubmittedAt == null) {
            return -1;
        }
        if (thisSubmittedAt == null && thatSubmittedAt != null) {
            return 1;
        }
        return thisSubmittedAt.compareTo(thatSubmittedAt);
    }
}
