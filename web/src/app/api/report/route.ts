import { execFile } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";
import { promisify } from "node:util";

const run = promisify(execFile);

const repo = path.resolve(process.cwd(), "..");
const jar = path.join(repo, "java", "build", "libs", "mark.jar");
const fixtures = path.join(repo, "spec", "fixtures");

export const dynamic = "force-dynamic";

// Only a fixture folder name, so a request cannot point Java at another path.
const CASE_NAME = /^[a-z0-9-]+$/;

export async function GET(request: Request): Promise<Response> {
  const name = new URL(request.url).searchParams.get("case") ?? "walkthrough";
  const caseFolder = path.join(fixtures, name);
  if (!CASE_NAME.test(name) || !existsSync(path.join(caseFolder, "activities.csv"))) {
    return Response.json({ problem: `No fixture named ${name}` }, { status: 404 });
  }
  if (!existsSync(jar)) {
    return Response.json(
      { problem: "No jar. Run ./gradlew jar in java/ first." },
      { status: 500 },
    );
  }
  try {
    const { stdout } = await run("java", ["-jar", jar, caseFolder]);
    return new Response(stdout, { headers: { "content-type": "application/json" } });
  } catch (error) {
    const stderr = (error as { stderr?: string }).stderr ?? String(error);
    return Response.json({ problem: "Java failed", stderr }, { status: 500 });
  }
}
