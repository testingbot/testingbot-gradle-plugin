# Security Policy

## Supported versions

The latest released `0.x` version receives security fixes. Older versions are not
maintained — please upgrade to the newest release.

| Version | Supported |
|---------|-----------|
| 0.1.x   | ✅        |
| < 0.1   | ❌        |

## Reporting a vulnerability

**Please do not open a public GitHub issue for security problems.**

Report vulnerabilities privately via either:

- GitHub's [private vulnerability reporting](https://github.com/testingbot/testingbot-gradle-plugin/security/advisories/new)
  (Security → Report a vulnerability), or
- email **security@testingbot.com**.

Please include:

- a description of the issue and its impact,
- steps to reproduce or a proof of concept,
- affected version(s) and environment details.

We aim to acknowledge reports within **3 business days** and to provide a remediation
timeline after triage. We will credit reporters who wish to be acknowledged once a fix is
released.

## Handling of credentials

This plugin sends your TestingBot API key and secret to the TestingBot API over HTTPS using
HTTP Basic authentication. Credentials are read from the DSL, Gradle properties, or
environment variables and are **never** written to build outputs or logs. Prefer providing
them through environment variables / CI secrets, and never commit them to source control.
