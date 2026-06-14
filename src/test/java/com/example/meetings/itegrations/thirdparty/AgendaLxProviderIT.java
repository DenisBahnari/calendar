package com.example.meetings.itegrations.thirdparty;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.DiscoveredEvent;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgendaLxProviderIT {

    private MockWebServer server;
    private AgendaLxProvider provider;

    @BeforeEach
    void setup() throws Exception {

        server = new MockWebServer();
        server.start();

        RestClient client = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();

        provider = new AgendaLxProvider(client);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldConvertAgendaResponseIntoDiscoveredEvent() throws Exception {

        String json = """
                [
                  {
                    "id": 237780,
                    "title": {
                      "rendered": "Mari Froes"
                    },
                    "string_times": "sáb: 20h30",
                    "description": [
                      "A cantora brasileira Mari Froes atua em Lisboa."
                    ],
                    "venue": {
                      "monsantos-open-air": {
                        "id": 4208,
                        "name": "Monsantos Open Air"
                      }
                    },
                    "link": "https://www.agendalx.pt/events/event/mari-froes/",
                    "occurences": [
                      "2099-06-13"
                    ]
                  }
                ]
                """;

        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(json)
        );

        List<DiscoveredEvent> events = provider.search("mari");

        assertEquals(1, events.size());

        DiscoveredEvent event = events.get(0);

        assertEquals("Agenda Cultural de Lisboa", event.source());
        assertEquals("237780", event.externalId());
        assertEquals("Mari Froes", event.title());
        assertEquals("Monsantos Open Air", event.venue());
        assertEquals("https://www.agendalx.pt/events/event/mari-froes/", event.url());
        assertTrue(event.description().contains("Mari Froes"));
        assertNotNull(event.start());
        assertEquals(20, event.start()
                        .atZone(ZoneId.of("Europe/Lisbon")).getHour());
        assertEquals(30, event.start()
                        .atZone(ZoneId.of("Europe/Lisbon")).getMinute());
    }

    @Test
    void shouldReturnEmptyListWhenProviderReturnsError() {

        server.enqueue(
                new MockResponse()
                        .setResponseCode(500)
        );

        List<DiscoveredEvent> events = provider.search("mari");

        assertTrue(events.isEmpty());
    }

    @Test
    void shouldIgnoreEventsWithoutFutureOccurrence() {

        String json = """
                [
                  {
                    "id": 1,
                    "title": {
                      "rendered": "Past Event"
                    },
                    "occurences": [
                      "2020-01-01"
                    ]
                  }
                ]
                """;

        server.enqueue(new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(json)
        );

        List<DiscoveredEvent> events = provider.search("past");
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldFallbackTo20hWhenTimeCannotBeParsed() {

        String json = """
                [
                  {
                    "id": 1,
                    "title": {
                      "rendered": "No Time Event"
                    },
                    "string_times": "unknown",
                    "occurences": [
                      "2099-01-01"
                    ]
                  }
                ]
                """;

        server.enqueue(new MockResponse()
                        .setHeader("Content-Type", "application/json").setBody(json)
        );

        List<DiscoveredEvent> events = provider.search("time");

        assertEquals(1, events.size());
        DiscoveredEvent event = events.get(0);
        assertEquals(20, event.start()
                        .atZone(ZoneId.of("Europe/Lisbon")).getHour());

        assertEquals(0, event.start().atZone(ZoneId.of("Europe/Lisbon")).getMinute());
    }

    @Test
    void shouldIgnoreEventsWithoutTitle() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                [
                  {
                    "id": 123,
                    "title": {},
                    "description": ["Some description"],
                    "string_times": "20h30",
                    "occurences": ["2030-01-01"],
                    "link": "https://example.com/event",
                    "venue": {
                      "venue-1": {
                        "name": "Test Venue"
                      }
                    }
                  }
                ]
                """));

        List<DiscoveredEvent> events = provider.search("test");
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldStripHtmlFromDescription() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                [
                  {
                    "id": 456,
                    "title": {
                      "rendered": "HTML Event"
                    },
                    "description": [
                      "<p>Hello <em>world</em></p>"
                    ],
                    "string_times": "21h00",
                    "occurences": ["2030-01-01"],
                    "link": "https://example.com/html",
                    "venue": {
                      "venue-1": {
                        "name": "HTML Venue"
                      }
                    }
                  }
                ]
                """));

        List<DiscoveredEvent> events = provider.search("html");

        assertEquals(1, events.size());

        DiscoveredEvent event = events.get(0);

        assertNotNull(event.description());
        assertFalse(event.description().contains("<"));
        assertFalse(event.description().contains(">"));
        assertTrue(event.description().contains("Hello"));
        assertTrue(event.description().contains("world"));
    }
}