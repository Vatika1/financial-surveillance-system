---
description: Run the Phase 2 Kafka hardening audit on a specific service
argument-hint: <service-name>
allowed-tools: Agent, AskUserQuestion
---

Run the Phase 2 Kafka hardening audit on a service.

Argument provided: `$ARGUMENTS`

Steps:

1. **Resolve the target service.** If `$ARGUMENTS` is empty or whitespace, use `AskUserQuestion` to ask which service to audit. Offer these four single-select options:
   - `trade-ingestion`
   - `activity-monitor`
   - `alert-service`
   - `case-management`

2. **Validate.** The service name must be exactly one of the four above. If not, report the invalid value to the user and stop — do **not** invoke the subagent.

3. **Invoke the auditor.** Call the `Agent` tool with `subagent_type: "kafka-phase2-auditor"` and this prompt (substitute `<service-name>` with the validated value):

   > Audit the `<service-name>-service` module for Phase 2 Kafka hardening compliance. Run the producer, consumer, topic-config, and manifest checklists from your instructions in full. Cite every finding with `file:line`. Produce the standard report format. Do not modify any files.

4. **Return the report verbatim** to the user. Do not summarise or paraphrase the auditor's findings.
