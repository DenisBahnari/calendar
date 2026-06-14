package com.example.meetings.integrations.thirdparty;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.TicketmasterProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketmasterProviderIT {

    private MockWebServer server;
    private TicketmasterProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        RestClient client = RestClient.builder()
                .baseUrl(server.url("/").toString()).build();

        provider = new TicketmasterProvider("fake-api-key", "PT", client);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldMapTicketmasterEvent() {

        String body = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "abc123",
                    "name": "Jazz Festival",
                    "url": "https://ticketmaster.com/jazz",
                    "info": "Great jazz event",
                    "dates": {
                      "start": {
                        "dateTime": "2027-06-20T20:00:00Z"
                      }
                    },
                    "_embedded": {
                      "venues": [
                        {
                          "name": "Lisbon Arena"
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(body)
        );

        List<DiscoveredEvent> events = provider.search("jazz");

        assertEquals(1, events.size());

        DiscoveredEvent e = events.get(0);

        assertEquals("Ticketmaster", e.source());
        assertEquals("abc123", e.externalId());
        assertEquals("Jazz Festival", e.title());
        assertEquals("Great jazz event", e.description());
        assertEquals("Lisbon Arena", e.venue());
        assertEquals("https://ticketmaster.com/jazz", e.url());
        assertEquals(Instant.parse("2027-06-20T20:00:00Z"), e.start());
    }

    @Test
    void shouldReturnEmptyWhenNoEvents() {

        String body = """
            {
              "_embedded": {
                "events": []
              }
            }
            """;

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(body)
        );

        List<DiscoveredEvent> events = provider.search("jazz");
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldIgnoreEventsWithoutDateTime() {

        String body = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "1",
                    "name": "Coachella",
                    "dates": {
                      "start": {
                        "localDate": "2027-04-09",
                        "timeTBA": true
                      }
                    }
                  }
                ]
              }
            }
            """;

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(body)
        );

        List<DiscoveredEvent> events = provider.search("music");
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldReturnEmptyOnServerError() {

        server.enqueue(
                new MockResponse().setResponseCode(500)
        );

        List<DiscoveredEvent> events = provider.search("jazz");
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldHandleMissingVenue() {

        String body = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "1",
                    "name": "Jazz Night",
                    "url": "https://ticketmaster.com/jazz-night",
                    "info": "Live jazz",
                    "dates": {
                      "start": {
                        "dateTime": "2027-06-20T20:00:00Z"
                      }
                    }
                  }
                ]
              }
            }
            """;

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(body)
        );

        List<DiscoveredEvent> events = provider.search("jazz");
        assertEquals(1, events.size());
        assertNull(events.get(0).venue());
    }

    @Test
    void shouldHandleMissingEmbeddedSection() {

        String body = """
            {
            }
            """;

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(body)
        );

        List<DiscoveredEvent> events = provider.search("jazz");
        assertTrue(events.isEmpty());
    }
}
