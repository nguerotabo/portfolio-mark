package mark;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

/**
 * Checks the keys {@code expected} names. A skipped path, and everything under it, is left
 * unchecked. Message wording on an error or a warning is never compared.
 */
final class ReportCompare {
  private ReportCompare() {}

  static void compare(String fixture, JsonNode expected, JsonNode actual, SkipSet skipped) {
    compareNode(fixture, "", expected, actual, skipped);
  }

  static boolean fullySkipped(JsonNode expected, SkipSet skipped) {
    return stillChecked(expected, skipped).isEmpty();
  }

  /** Top-level report keys that still have something to check. */
  static List<String> stillChecked(JsonNode expected, SkipSet skipped) {
    List<String> names = new ArrayList<>();
    Iterator<String> fields = expected.fieldNames();
    while (fields.hasNext()) {
      String name = fields.next();
      if (hasUnchecked(expected.get(name), name, skipped)) {
        names.add(name);
      }
    }
    return List.copyOf(names);
  }

  private static boolean hasUnchecked(JsonNode node, String path, SkipSet skipped) {
    if (skipped.covers(path)) {
      return false;
    }
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      boolean anyChild = false;
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (field.getKey().equals("message") && messageIgnored(path)) {
          continue;
        }
        anyChild = true;
        if (hasUnchecked(field.getValue(), path + "." + field.getKey(), skipped)) {
          return true;
        }
      }
      return !anyChild;
    }
    if (node.isArray()) {
      return true;
    }
    return true;
  }

  private static void compareNode(
      String fixture, String path, JsonNode expected, JsonNode actual, SkipSet skipped) {
    if (!path.isEmpty() && skipped.covers(path)) {
      return;
    }
    if (expected.isObject()) {
      if (actual == null || !actual.isObject()) {
        fail(fixture, path, "an object", actual);
      }
      Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        String child = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
        if (field.getKey().equals("message") && messageIgnored(path)) {
          continue;
        }
        if (skipped.covers(child)) {
          continue;
        }
        JsonNode actualChild = actual.get(field.getKey());
        if (actualChild == null) {
          fail(fixture, child, field.getValue(), null);
        }
        compareNode(fixture, child, field.getValue(), actualChild, skipped);
      }
      return;
    }
    if (expected.isArray()) {
      if (actual == null || !actual.isArray()) {
        fail(fixture, path, "an array", actual);
      }
      if (expected.size() != actual.size()) {
        Assertions.fail(
            label(fixture, path)
                + ": expected "
                + expected.size()
                + " items but was "
                + actual.size());
      }
      for (int i = 0; i < expected.size(); i++) {
        compareNode(fixture, path + "." + i, expected.get(i), actual.get(i), skipped);
      }
      return;
    }
    if (!sameValue(expected, actual)) {
      fail(fixture, path, expected, actual);
    }
  }

  private static boolean messageIgnored(String objectPath) {
    return objectPath.equals("error") || objectPath.matches("warnings\\.\\d+");
  }

  private static boolean sameValue(JsonNode expected, JsonNode actual) {
    if (expected == null || actual == null) {
      return false;
    }
    if (expected.isNumber() || actual.isNumber()) {
      return wholeNumber(expected)
          && wholeNumber(actual)
          && expected.bigIntegerValue().equals(actual.bigIntegerValue());
    }
    return expected.equals(actual);
  }

  private static boolean wholeNumber(JsonNode node) {
    if (!node.isNumber()) {
      return false;
    }
    return switch (node.numberType()) {
      case INT, LONG, BIG_INTEGER -> true;
      default -> false;
    };
  }

  private static void fail(String fixture, String path, Object expected, Object actual) {
    String shown = actual == null ? "missing" : actual.toString();
    Assertions.fail(label(fixture, path) + ": expected " + expected + " but was " + shown);
  }

  private static String label(String fixture, String path) {
    return path.isEmpty() ? fixture : fixture + " " + path;
  }
}
