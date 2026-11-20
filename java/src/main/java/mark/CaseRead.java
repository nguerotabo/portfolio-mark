package mark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mark.ledger.Activity;
import mark.ledger.CsvReader;
import mark.market.Market;

/**
 * One case folder. Without {@code run.json} the result is the activities and the warnings, or a
 * reader refusal. With {@code run.json}, {@code asOf} and {@code fund} are available too.
 */
public sealed interface CaseRead permits CaseRead.Refused, CaseRead.ActivitiesOnly, CaseRead.WithRun {

  record Warning(String code, Map<String, Object> fields, String message) {}

  record Refused(CsvReader.Error error) implements CaseRead {}

  record ActivitiesOnly(List<Activity> activities, List<Warning> warnings, Market.Book market)
      implements CaseRead {}

  record WithRun(
      List<Activity> activities,
      List<Warning> warnings,
      LocalDate asOf,
      String fund,
      Market.Book market)
      implements CaseRead {}

  static CaseRead read(Path caseFolder) {
    CsvReader activities = CsvReader.read(caseFolder.resolve("activities.csv"));
    if (activities instanceof CsvReader.Error error) {
      return new Refused(error);
    }
    List<Activity> rows = ((CsvReader.Success) activities).activities();
    Market.Book market = Market.read(caseFolder);
    List<Warning> warnings = new ArrayList<>();
    for (Market.Warning warning : market.warnings()) {
      warnings.add(new Warning(warning.code(), warning.fields(), warning.message()));
    }
    Path runFile = caseFolder.resolve("run.json");
    if (!Files.exists(runFile)) {
      return new ActivitiesOnly(rows, List.copyOf(warnings), market);
    }
    try {
      JsonNode node = new ObjectMapper().readTree(runFile.toFile());
      JsonNode asOf = node.get("asOf");
      JsonNode fund = node.get("fund");
      if (asOf == null || fund == null || !asOf.isTextual() || !fund.isTextual()) {
        throw new IllegalArgumentException("run.json needs asOf and fund as text: " + runFile);
      }
      LocalDate asOfDate = LocalDate.parse(asOf.asText());
      if (!rows.isEmpty() && asOfDate.isBefore(rows.get(rows.size() - 1).date())) {
        LocalDate last = rows.get(rows.size() - 1).date();
        return new Refused(
            new CsvReader.Error(
                "as-of",
                Map.of("asOf", asOfDate.toString(), "lastActivity", last.toString()),
                "As-of date " + asOfDate + " is before the last activity " + last + "."));
      }
      return new WithRun(rows, List.copyOf(warnings), asOfDate, fund.asText(), market);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
