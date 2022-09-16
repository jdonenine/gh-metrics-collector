package com.onenine.ghmc.models;

import com.onenine.ghmc.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.slf4j.Logger;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

import static org.slf4j.LoggerFactory.getLogger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullCommit implements Comparable<PullCommit> {
    private static final Logger log = getLogger(PullCommit.class);

    private String org;
    private String repo;
    private Integer number;
    private String author;
    private String committer;
    @Field(type = FieldType.Date)
    private ZonedDateTime authoredAt;
    @Field(type = FieldType.Date)
    private ZonedDateTime committedAt;

    public static PullCommit fromGHPullRequestCommitDetail(String org, String repo, Integer number, GHPullRequestCommitDetail ghCommitDetail) {
        if (ghCommitDetail == null) {
            return null;
        }

        PullCommit commit = new PullCommit();

        commit.setOrg(org);

        commit.setRepo(repo);

        commit.setNumber(number);

        GHPullRequestCommitDetail.Commit ghCommit = null;
        try {
            ghCommit = ghCommitDetail.getCommit();
        } catch (Exception e) {
            log.warn("Unable to extract commit from commit details for {}/{}/{}/{}", org, repo, number, ghCommitDetail.getSha(), e);
        }

        if (ghCommit == null) {
            return commit;
        }

        try {
            commit.setAuthoredAt(TimeUtils.getUtcDateTimeFromDate(ghCommit.getAuthor().getDate()));
        } catch (Exception e) {
            log.warn("Unable to extract authoredAt for commit details for {}/{}/{}/{}", org, repo, number, ghCommitDetail.getSha(), e);
        }

        try {
            commit.setCommittedAt(TimeUtils.getUtcDateTimeFromDate(ghCommit.getCommitter().getDate()));
        } catch (Exception e) {
            log.warn("Unable to extract committedAt for commit details for {}/{}/{}/{}", org, repo, number, ghCommitDetail.getSha(), e);
        }

        try {
            commit.setCommitter(ghCommit.getCommitter().getName());
        } catch (Exception e) {
            log.warn("Unable to extract committer for commit details for {}/{}/{}/{}", org, repo, number, ghCommitDetail.getSha(), e);
        }

        try {
            commit.setAuthor(ghCommit.getAuthor().getName());
        } catch (Exception e) {
            log.warn("Unable to extract committer for commit details for {}/{}/{}/{}", org, repo, number, ghCommitDetail.getSha(), e);
        }

        return commit;
    }

    @Override
    public int compareTo(PullCommit that) {
        ZonedDateTime thisCommittedAt = getCommittedAt();
        ZonedDateTime thatCommittedAt = that != null ? that.getCommittedAt() : null;
        if (thisCommittedAt == null && thatCommittedAt == null) {
            return 0;
        }
        if (thisCommittedAt != null && thatCommittedAt == null) {
            return -1;
        }
        if (thisCommittedAt == null && thatCommittedAt != null) {
            return 1;
        }
        return thisCommittedAt.compareTo(thatCommittedAt);
    }
}
