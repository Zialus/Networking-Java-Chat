# Networking Java Chat

[![CodSpeed](https://img.shields.io/endpoint?url=https://codspeed.io/badge.json)](https://app.codspeed.io/Zialus/Networking-Java-Chat?utm_source=badge)

## Benchmarks

JMH benchmarks live in `src/jmh/java` and are run in CI with
[CodSpeed](https://codspeed.io) through the CodSpeed JMH fork, checked out as a
git submodule in `third-party/codspeed-jvm`.

```bash
git submodule update --init --recursive
gradle jmh
```
