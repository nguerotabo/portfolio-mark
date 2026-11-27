package mark;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One case folder in, its report as JSON on stdout. A refusal is a report too, so it also exits 0.
 * Only a missing or unreadable folder exits non-zero.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("usage: java -jar mark.jar <case-folder>");
      System.exit(2);
    }
    Path caseFolder = Path.of(args[0]);
    if (!Files.isRegularFile(caseFolder.resolve("activities.csv"))) {
      System.err.println("No activities.csv in " + caseFolder);
      System.exit(2);
    }
    System.out.println(Cases.reportJson(caseFolder));
  }
}
