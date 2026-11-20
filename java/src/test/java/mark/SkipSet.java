package mark;

import java.util.Set;

/** Field paths the runner leaves unchecked for one fixture. A path covers its children. */
final class SkipSet {
  private final Set<String> fields;

  private SkipSet(Set<String> fields) {
    this.fields = fields;
  }

  static SkipSet none() {
    return new SkipSet(Set.of());
  }

  static SkipSet of(String... fields) {
    return new SkipSet(Set.of(fields));
  }

  static SkipSet of(Set<String> fields) {
    return new SkipSet(Set.copyOf(fields));
  }

  boolean covers(String path) {
    for (String field : fields) {
      if (path.equals(field) || path.startsWith(field + ".")) {
        return true;
      }
    }
    return false;
  }
}
