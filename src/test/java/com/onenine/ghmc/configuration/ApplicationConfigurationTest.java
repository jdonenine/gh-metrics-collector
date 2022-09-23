package com.onenine.ghmc.configuration;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;

public class ApplicationConfigurationTest {
    @Test
    public void testInvalidConfiguration() {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        assertEquals(false, applicationConfiguration.isValid());

        applicationConfiguration.setGhAccessToken("test-token");
        assertEquals(false, applicationConfiguration.isValid());

        applicationConfiguration.setGhOrg("test-org");
        assertEquals(false, applicationConfiguration.isValid());
    }

    @Test
    public void testValidConfiguration() {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.setGhAccessToken("test-token");
        applicationConfiguration.setGhOrg("test-org");
        applicationConfiguration.setGhRepo("test-repo");
        assertEquals(true, applicationConfiguration.isValid());
    }
}
