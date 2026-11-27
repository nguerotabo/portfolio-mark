package mark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import mark.analytics.Bridge;
import mark.analytics.Picks;
import mark.ledger.Activity;
import mark.ledger.CsvReader;
import mark.ledger.Dividend;
import mark.ledger.ReportRow;

/**
 * One case folder in, its report as JSON text.
 *
 * <p>Proves the activities of deposit-quantity, dividend-quantity, cash-check, and same-second, and
 * the errors of refuse-column, refuse-currency, refuse-account, refuse-type, refuse-withdrawal,
 * refuse-cash, refuse-cash-below-zero, and refuse-as-of. With {@code run.json}, also the paths,
 * steps, headline, and picks of buys-only, half-cent, walkthrough, weekend-deposit, and
 * as-of-input, and refuse-missing-close and refuse-oversell.
 */
public final class Cases {
  private static final ObjectMapper JSON = new ObjectMapper();

  private Cases() {}

  public static String reportJson(Path caseFolder) {
    try {
      return write(CaseRead.read(caseFolder));
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static String write(CaseRead read) throws JsonProcessingException {
    ObjectNode root = JSON.createObjectNode();
    if (read instanceof CaseRead.Refused refused) {
      root.set("error", errorNode(refused.error()));
      return JSON.writeValueAsString(root);
    }
    if (read instanceof CaseRead.ActivitiesOnly only) {
      root.set("activities", activitiesNode(only.activities()));
      root.set("warnings", warningsNode(only.warnings()));
      return JSON.writeValueAsString(root);
    }
    CaseRead.WithRun run = (CaseRead.WithRun) read;
    if (!replayBuilt(run.activities())) {
      root.put("asOf", run.asOf().toString());
      root.put("fund", run.fund());
      root.set("activities", activitiesNode(run.activities()));
      root.set("warnings", warningsNode(run.warnings()));
      return JSON.writeValueAsString(root);
    }
    Bridge.Result result =
        Bridge.calculate(run.activities(), run.market(), run.fund(), run.asOf());
    if (result instanceof Bridge.Refused refused) {
      root.set("error", errorNode(refused.error()));
      return JSON.writeValueAsString(root);
    }
    Bridge.Built built = (Bridge.Built) result;
    Bridge bridge = built.bridge();
    Picks.Replay replay = built.replay();
    root.put("asOf", run.asOf().toString());
    root.put("fund", run.fund());
    ObjectNode paths = root.putObject("paths");
    paths.put("allOnDayOne", bridge.allOnDayOne());
    paths.put("fundOnYourDates", bridge.fundOnYourDates());
    paths.put("benchmark", bridge.benchmark());
    paths.put("account", bridge.account());
    ObjectNode steps = root.putObject("steps");
    steps.put("whenMoneyArrived", bridge.whenMoneyArrived());
    steps.put("cashAndSales", bridge.cashAndSales());
    steps.put("stockSelection", bridge.stockSelection());
    root.put("headline", bridge.headline());
    root.set("picks", picksNode(replay.picks()));
    root.put("bestPick", replay.bestPick());
    root.put("largestDrag", replay.largestDrag());
    root.set("activities", activitiesNode(run.activities()));
    root.set("warnings", warningsNode(run.warnings()));
    return JSON.writeValueAsString(root);
  }

  /** Dividends wait for the replay week that builds them. */
  private static boolean replayBuilt(List<Activity> activities) {
    for (Activity activity : activities) {
      if (activity instanceof Dividend) {
        return false;
      }
    }
    return true;
  }

  private static ArrayNode picksNode(List<Picks.Pick> picks) {
    ArrayNode rows = JSON.createArrayNode();
    for (Picks.Pick pick : picks) {
      ObjectNode node = rows.addObject();
      node.put("symbol", pick.symbol());
      node.put("impact", pick.impact());
      node.put("dividends", pick.dividends());
      ArrayNode lots = node.putArray("lots");
      for (Picks.Lot lot : pick.lots()) {
        lots.addObject().put("account", lot.account()).put("fund", lot.fund());
      }
    }
    return rows;
  }

  private static ArrayNode activitiesNode(List<Activity> activities) {
    ArrayNode rows = JSON.createArrayNode();
    for (ReportRow row : ReportRow.from(activities)) {
      rows.add(JSON.valueToTree(row));
    }
    return rows;
  }

  private static ArrayNode warningsNode(List<CaseRead.Warning> warnings) {
    ArrayNode rows = JSON.createArrayNode();
    for (CaseRead.Warning warning : warnings) {
      ObjectNode node = rows.addObject();
      node.put("code", warning.code());
      node.set("fields", fieldsNode(warning.fields()));
      node.put("message", warning.message());
    }
    return rows;
  }

  private static ObjectNode errorNode(CsvReader.Error error) {
    ObjectNode node = JSON.createObjectNode();
    node.put("code", error.code());
    node.set("fields", fieldsNode(error.fields()));
    node.put("message", error.message());
    return node;
  }

  private static ObjectNode fieldsNode(Map<String, Object> fields) {
    ObjectNode node = JSON.createObjectNode();
    for (Map.Entry<String, Object> field : fields.entrySet()) {
      Object value = field.getValue();
      if (value instanceof Integer || value instanceof Long) {
        node.put(field.getKey(), ((Number) value).longValue());
      } else if (value instanceof String text) {
        node.put(field.getKey(), text);
      } else {
        throw new IllegalArgumentException(
            "Report field " + field.getKey() + " is " + value.getClass().getName());
      }
    }
    return node;
  }
}
