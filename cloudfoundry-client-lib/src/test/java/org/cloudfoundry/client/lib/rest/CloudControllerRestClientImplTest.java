package org.cloudfoundry.client.lib.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CloudControllerRestClientImplTest {

    private CloudControllerRestClientImpl controllerClient;

    // @formatter:off
    private static Stream<Arguments> testExtractUriInfo() {
        return Stream.of(
                Arguments.of("domain-1.json", null),
                Arguments.of("domain-2.json", null),
                Arguments.of("domain-3.json", null),
                Arguments.of("domain-4.json", IllegalArgumentException.class),
                Arguments.of("domain-5.json", null),
                Arguments.of("domain-6.json", null),
                Arguments.of("domain-7.json", IllegalArgumentException.class)
        );
    }
    // @formatter:on

    @BeforeEach
    public void setUpWithEmptyConstructor() {
        controllerClient = new CloudControllerRestClientImpl();
    }

    @ParameterizedTest
    @MethodSource
    public void testExtractUriInfo(String fileName, Class<? extends RuntimeException> expectedException) throws Throwable {
        String fileContent = TestUtil.readFileContent(fileName, getClass());
        Input input = TestUtil.fromJson(fileContent, Input.class);
        Map<String, String> uriInfo = new HashMap<>();
        Map<String, UUID> domainsAsMap = input.getDomainsAsMap();

        executeWithErrorHandling(() -> controllerClient.extractUriInfo(domainsAsMap, input.getUri(), uriInfo), expectedException, input.getErrorMessage());

        validateUriInfo(uriInfo, domainsAsMap, input.getExpectedDomain(), input.getExpectedHost(), input.getExpectedPath());
    }

    private void executeWithErrorHandling(Executable executable, Class<? extends RuntimeException> expectedException, String exceptionMessage) throws Throwable {
        if (expectedException != null) {
            RuntimeException runtimeException = Assertions.assertThrows(expectedException, executable);
            Assertions.assertEquals(exceptionMessage, runtimeException.getMessage());
            return;
        }
        executable.execute();
    }

    private void validateUriInfo(Map<String, String> uriInfo, Map<String, UUID> domainsAsMap, String expectedDomain, String expectedHost,
                                 String expectedPath) {
        Assertions.assertEquals(domainsAsMap.get(expectedDomain), domainsAsMap.get(uriInfo.get("domainName")));
        Assertions.assertEquals(expectedHost, uriInfo.get("host"));
        Assertions.assertEquals(expectedPath, uriInfo.get("path"));
    }

    private static class Input {
        private String uri;
        private List<Domain> domains;
        private String expectedPath;
        private String expectedHost;
        private String expectedDomain;
        private String errorMessage;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public List<Domain> getDomains() {
            return domains;
        }

        public void setDomains(List<Domain> domains) {
            this.domains = domains;
        }

        public String getExpectedPath() {
            return expectedPath;
        }

        public void setExpectedPath(String expectedPath) {
            this.expectedPath = expectedPath;
        }

        public String getExpectedHost() {
            return expectedHost;
        }

        public void setExpectedHost(String expectedHost) {
            this.expectedHost = expectedHost;
        }

        public String getExpectedDomain() {
            return expectedDomain;
        }

        public void setExpectedDomain(String expectedDomain) {
            this.expectedDomain = expectedDomain;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Map<String, UUID> getDomainsAsMap() {
            return getDomains().stream()
                               .collect(Collectors.toMap(Domain::getName, Domain::getUuid));
        }

        private static class Domain {
            private String name;
            private UUID uuid;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public UUID getUuid() {
                return uuid;
            }

            public void setUuid(UUID uuid) {
                this.uuid = uuid;
            }
        }
    }
}
