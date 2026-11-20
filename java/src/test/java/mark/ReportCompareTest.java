package mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/** The comparer ignores message wording and skipped fields. Values here are not fixture answers. */
final class ReportCompareTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void errorMessageWordingIsNotCompared() throws Exception {
    JsonNode expected =
        json.readTree(
            "{\"error\":{\"code\":\"column\",\"fields\":{\"column\":\"symbol\"},\"message\":\"one\"}}");
    JsonNode actual =
        json.readTree(
            "{\"error\":{\"code\":\"column\",\"fields\":{\"column\":\"symbol\"},\"message\":\"two\"}}");
    ReportCompare.compare("sample", expected, actual, SkipSet.none());
  }

  @Test
  void warningMessageWordingIsNotCompared() throws Exception {
    JsonNode expected =
        json.readTree(
            "{\"warnings\":[{\"code\":\"price-jump\",\"fields\":{\"symbol\":\"ABC\"},\"message\":\"one\"}]}");
    JsonNode actual =
        json.readTree(
            "{\"warnings\":[{\"code\":\"price-jump\",\"fields\":{\"symbol\":\"ABC\"},\"message\":\"two\"}]}");
    ReportCompare.compare("sample", expected, actual, SkipSet.none());
  }

  @Test
  void aDifferentErrorCodeFails() throws Exception {
    JsonNode expected = json.readTree("{\"error\":{\"code\":\"column\",\"fields\":{}}}");
    JsonNode actual = json.readTree("{\"error\":{\"code\":\"type\",\"fields\":{}}}");
    AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> ReportCompare.compare("sample", expected, actual, SkipSet.none()));
    assertTrue(error.getMessage().contains("sample error.code"));
  }

  @Test
  void aSkippedFieldIsLeftUnchecked() throws Exception {
    JsonNode expected = json.readTree("{\"activities\":[],\"paths\":{\"account\":1}}");
    JsonNode actual = json.readTree("{\"activities\":[]}");
    ReportCompare.compare("sample", expected, actual, SkipSet.of("paths"));
    assertEquals(List.of("activities"), ReportCompare.stillChecked(expected, SkipSet.of("paths")));
  }

  @Test
  void aFractionalNumberDoesNotMatchAWholeNumber() throws Exception {
    JsonNode expected = json.readTree("{\"headline\":1}");
    JsonNode actual = json.readTree("{\"headline\":1.0}");
    assertThrows(
        AssertionFailedError.class,
        () -> ReportCompare.compare("sample", expected, actual, SkipSet.none()));
  }

  @Test
  void everyListedTopLevelFieldMeansTheFixtureIsFullySkipped() throws Exception {
    JsonNode expected = json.readTree("{\"activities\":[{\"cash\":1}],\"warnings\":[]}");
    SkipSet skipped = SkipSet.of("activities", "warnings");
    assertTrue(ReportCompare.fullySkipped(expected, skipped));
    assertFalse(ReportCompare.fullySkipped(expected, SkipSet.of("warnings")));
  }
}
