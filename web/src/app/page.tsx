"use client";

import { useEffect, useState } from "react";

export default function Page() {
  const [report, setReport] = useState("Running Java…");

  useEffect(() => {
    const name = new URLSearchParams(window.location.search).get("case") ?? "walkthrough";
    fetch(`/api/report?case=${encodeURIComponent(name)}`)
      .then((response) => response.text())
      .then((text) => setReport(JSON.stringify(JSON.parse(text), null, 2)))
      .catch((error) => setReport(String(error)));
  }, []);

  return <pre>{report}</pre>;
}
