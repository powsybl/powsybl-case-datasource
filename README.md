# PowSyBl case datasource

[![Actions Status](https://github.com/powsybl/powsybl-case-datasource/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/powsybl/powsybl-case-datasource/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-case-datasource&metric=coverage)](https://sonarcloud.io/component_measures?id=com.powsybl%3Apowsybl-case-datasource&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-36jvd725u-cnquPgZb6kpjH8SKh~FWHQ)

## Description

**powsybl-case-datasource** is a small Java library that provides a PowSyBl `ReadOnlyDataSource` implementation
backed by a remote **case-server**. It lets any PowSyBl-based application load a network case (and its auxiliary
files) over HTTP, without needing direct filesystem or storage access to the case.


## Technical Stack

- Java / Spring (`RestClient`, `@Component`)
- PowSyBl Core (`ReadOnlyDataSource`)
- JUnit 5 / Mockito for tests

## How It Works

`CaseDataSourceClient` implements `com.powsybl.commons.datasource.ReadOnlyDataSource` and translates each datasource
operation into a REST call to the `case-server` `/v1/cases/{caseUuid}/datasource/...` API:

| `ReadOnlyDataSource` method | Case-server endpoint called |
|---|---|
| `getBaseName()` | `GET /v1/cases/{caseUuid}/datasource/baseName` |
| `exists(String fileName)` / `exists(String suffix, String ext)` | `GET /v1/cases/{caseUuid}/datasource/exists` |
| `newInputStream(String fileName)` / `newInputStream(String suffix, String ext)` | `GET /v1/cases/{caseUuid}/datasource` |
| `listNames(String regex)` | `GET /v1/cases/{caseUuid}/datasource/list` |


Any `HttpStatusCodeException` or I/O error raised while calling the case-server is wrapped and rethrown as a
`CaseDataSourceClientException`.

## Usage

Add the dependency (version managed by the `powsybl-dependencies` BOM):

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-case-datasource-client</artifactId>
</dependency>
```

Create a client for a given case, then use it anywhere a `ReadOnlyDataSource` is expected:

```java
CaseDataSourceClient dataSource = new CaseDataSourceClient("http://case-server/", caseUuid);

Network network = Network.read(dataSource.getBaseName(), dataSource);
```

The base URL can also be injected from configuration via `${case-server.base.url}` (defaults to
`http://case-server/`), and the client can be reused for another case with `setCaseName(UUID)`.

