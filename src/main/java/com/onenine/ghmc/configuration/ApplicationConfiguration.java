package com.onenine.ghmc.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("application")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApplicationConfiguration {
    private String ghAccessToken;
    private String ghOrg;
    private String ghRepo;
    private String excludeCommentsFromGhUsers;
    private String excludeIssuesFromGhUsers;
    private String aviatorMergeQueueAuthor;
    private ElasticsearchConfiguration elasticsearch;
    private Boolean syncOnStartup;
    private Boolean exitAfterSyncOnStartup;
    private Integer defaultSyncHistoryWindowDays = 7;
    private String overrideSyncHistoryWindowStartDate;

    public Boolean isValid() {
        if (ghAccessToken == null || ghAccessToken.isEmpty()) {
            return false;
        }
        if (ghOrg == null || ghOrg.isEmpty()) {
            return false;
        }
        return ghRepo != null && !ghRepo.isEmpty();
    }

    public HashSet<String> getExcludeIssuesFromGithubUsersHashSet() {
        return buildHashSetFromCommaSeparatedList(excludeIssuesFromGhUsers);
    }

    public HashSet<String> getExcludeCommentsFromGithubUsersHashSet() {
        return buildHashSetFromCommaSeparatedList(excludeCommentsFromGhUsers);
    }

    private static HashSet<String> buildHashSetFromCommaSeparatedList(String list) {
        if (list == null || list.isEmpty()) {
            return Sets.newHashSet();
        }
        HashSet<String> set = Sets.newHashSet();
        String[] pieces = list.trim().split(",");
        if (pieces != null && pieces.length > 0) {
            for (int i = 0; i < pieces.length; i++) {
                String piece = pieces[i];
                if (piece == null || piece.isEmpty()) {
                    continue;
                }
                set.add(piece.trim());
            }
        }
        return set;
    }
}
